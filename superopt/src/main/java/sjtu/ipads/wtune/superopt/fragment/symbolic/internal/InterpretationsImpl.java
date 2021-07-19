package sjtu.ipads.wtune.superopt.fragment.symbolic.internal;

import static java.util.Objects.requireNonNull;
import static sjtu.ipads.wtune.common.utils.Commons.listJoin;
import static sjtu.ipads.wtune.common.utils.FuncUtils.stream;
import static sjtu.ipads.wtune.sqlparser.plan.AttributeDef.localeInput;
import static sjtu.ipads.wtune.sqlparser.plan.AttributeDefBag.makeBag;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.LEFT_JOIN;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDef;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDefBag;
import sjtu.ipads.wtune.sqlparser.plan.Expr;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.schema.Column;
import sjtu.ipads.wtune.sqlparser.schema.Column.Flag;
import sjtu.ipads.wtune.superopt.fragment.symbolic.AttributeInterpretation;
import sjtu.ipads.wtune.superopt.fragment.symbolic.InputInterpretation;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Interpretation;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Interpretations;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Placeholder;
import sjtu.ipads.wtune.superopt.fragment.symbolic.PredicateInterpretation;
import sjtu.ipads.wtune.superopt.util.Constraints;
import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.PickFrom;
import sjtu.ipads.wtune.symsolver.core.PickSub;
import sjtu.ipads.wtune.symsolver.core.Reference;

public class InterpretationsImpl implements Interpretations {
  private final InterpretationsImpl base;
  private final Constraints constraints;
  private final Map<Placeholder, Interpretation> interpretations;

  private InterpretationsImpl(Constraints constraints) {
    this.base = null;
    this.constraints = constraints;
    this.interpretations = new IdentityHashMap<>();
  }

  private InterpretationsImpl(InterpretationsImpl base) {
    this.base = base;
    this.constraints = base.constraints();
    this.interpretations = new IdentityHashMap<>();
  }

  public static Interpretations build(Constraints constraints) {
    requireNonNull(constraints);
    return new InterpretationsImpl(constraints);
  }

  public static Interpretations build(Interpretations interpretations) {
    if (!(interpretations instanceof InterpretationsImpl)) throw new IllegalArgumentException();
    return new InterpretationsImpl((InterpretationsImpl) interpretations);
  }

  @Override
  public Constraints constraints() {
    return constraints;
  }

  @Override
  public InputInterpretation getInput(Placeholder placeholder) {
    return (InputInterpretation) get0(placeholder);
  }

  @Override
  public AttributeInterpretation getAttributes(Placeholder placeholder) {
    return ((AttributeInterpretation) get0(placeholder));
  }

  @Override
  public PredicateInterpretation getPredicate(Placeholder placeholder) {
    return ((PredicateInterpretation) get0(placeholder));
  }

  @Override
  public boolean assignInput(Placeholder placeholder, PlanNode node) {
    return assign0(placeholder, new InputInterpretationImpl(node));
  }

  @Override
  public boolean assignAttributes(Placeholder placeholder, List<AttributeDef> defs) {
    return assign0(placeholder, new AttributeInterpretationImpl(defs));
  }

  @Override
  public boolean assignPredicate(Placeholder placeholder, Expr expr) {
    return assign0(placeholder, new PredicateInterpretationImpl(expr));
  }

  @Override
  public boolean hasAssignment(Placeholder placeholder) {
    return interpretations.containsKey(placeholder)
        || (base != null && base.hasAssignment(placeholder));
  }

  private Interpretation get0(Placeholder placeholder) {
    final Interpretation interpretation = interpretations.get(placeholder);
    if (interpretation != null) return interpretation;
    else if (base != null) return base.get0(placeholder);
    else return null;
  }

  private <T> boolean assign0(Placeholder placeholder, Interpretation<T> assignment) {
    if (!assign1(placeholder, assignment)) return false;
    for (Placeholder p : constraints.equivalenceOf(placeholder))
      if (!assign1(p, assignment)) return false;

    return checkConstraints();
  }

  private <T> boolean assign1(Placeholder placeholder, Interpretation<T> assignment) {
    final Interpretation<T> existing = (Interpretation<T>) get0(placeholder);
    if (existing != null) {
      if (!existing.isCompatible(assignment)) return false;
      if (!assignment.shouldOverride(existing)) return true;
    }
    interpretations.put(placeholder, assignment);
    return true;
  }

  private boolean checkConstraints() {
    return stream(constraints).allMatch(this::checkConstraint);
  }

  private boolean checkConstraint(Constraint constraint) {
    if (constraint.kind() == Constraint.Kind.Reference)
      return checkReference((Reference<Placeholder, Placeholder>) constraint);
    else if (constraint.kind() == Constraint.Kind.PickFrom)
      return checkSource((PickFrom<Placeholder, Placeholder>) constraint);
    else if (constraint.kind() == Constraint.Kind.PickSub)
      return checkSub((PickSub<Placeholder>) constraint);
    else return true;
  }

  private boolean checkSource(PickFrom<Placeholder, Placeholder> constraint) {
    final AttributeInterpretation attrInter = getAttributes(constraint.p());
    if (attrInter == null) return true;

    final Placeholder[] ts = constraint.ts();
    final AttributeDefBag[] bags = new AttributeDefBag[ts.length];
    for (int i = 0; i < ts.length; i++) {
      final InputInterpretation inter = getInput(ts[i]);
      if (inter == null) return true;
      bags[i] = inter.object().definedAttributes();
    }
    final AttributeDefBag bag = makeBag(listJoin(bags)); // aggregated bag

    for (AttributeDef attr : attrInter.object())
      for (AttributeDef ref : attr.references())
        // by definition, we check the used attributes of `attr` instead of itself
        // example: select a.i + a.j as k from a
        // here `k` (a.i + a.j) are still considered belongs to table `a`
        if (bag.locate(ref) == -1) return false;

    return true;
  }

  private boolean checkReference(Reference<Placeholder, Placeholder> constraint) {
    final AttributeInterpretation referee = getAttributes(constraint.px());
    final AttributeInterpretation referred = getAttributes(constraint.py());

    if (referee == null || referred == null || referee == referred) return true;

    final List<AttributeDef> refereeRefs = referee.object();
    final List<AttributeDef> referredRefs = referred.object();

    if (refereeRefs.size() != referredRefs.size()) return false;

    final InputInterpretation refereeInput = getInput(constraint.tx());
    final PlanNode refereeInputNode = refereeInput == null ? null : refereeInput.object();
    final InputInterpretation referredInput = getInput(constraint.ty());
    final PlanNode referredInputNode = referredInput == null ? null : referredInput.object();

    final List<Column> refereeCols = new ArrayList<>(refereeRefs.size());
    final List<Column> referredCols = new ArrayList<>(referredRefs.size());

    for (int i = 0, bound = refereeRefs.size(); i < bound; i++) {
      final AttributeDef refereeAttr = refereeRefs.get(i);
      final AttributeDef referredAttr = referredRefs.get(i);

      if (refereeAttr == null || referredAttr == null) return false;
      if (refereeAttr == referredAttr) continue;

      final ColumnRef refereeCol = columnRefOf(refereeAttr, refereeInputNode);
      final ColumnRef referredCol = columnRefOf(referredAttr, referredInputNode);
      if (refereeCol == null || referredCol == null) return false;
      if (referredCol.filtered || refereeCol.nullable) return false;
      if (refereeCol.column == referredCol.column) continue;

      refereeCols.add(refereeCol.column);
      referredCols.add(referredCol.column);
    }

    if (refereeCols.size() != referredCols.size()) return false;

    for (Column refereeCol : refereeCols) if (!refereeCol.references(referredCols)) return false;
    return true;
  }

  private boolean checkSub(PickSub<Placeholder> constraint) {
    final AttributeInterpretation downstream = getAttributes(constraint.px());
    final AttributeInterpretation upstream = getAttributes(constraint.py());
    if (downstream == null || upstream == null || downstream == upstream) return true;

    final List<AttributeDef> downstreamAttrs = downstream.object();
    final List<AttributeDef> upstreamAttrs = upstream.object();

    for (AttributeDef downstreamAttr : downstreamAttrs)
      if (!upstreamAttrs.contains(downstreamAttr)) return false;

    return true;
  }

  private static ColumnRef columnRefOf(AttributeDef attr, PlanNode surface) {
    // Retrieve the native column of given `attr`, from the perspective of `surface` node.
    // If there are filters between the native input node and `surface` node, then returns null.
    // Example:
    // plan: Proj<t.id>(Input<t>), nativeColumnOf(t.id,Proj) -> Column{`t`.`id`}
    // plan: Proj<t.id>(Filter(Input<t>)), nativeColumnOf(t.id,Proj) -> null
    // plan: Proj<t.id>(Filter(Input<t>)), nativeColumnOf(t.id,Input) -> Column{`t`.`id`}

    if (surface == null) return new ColumnRef(attr.referredColumn(), false, false);

    final AttributeDef pivot = surface.definedAttributes().lookup(attr);
    if (pivot == null) return null;

    final PlanNode inputNode = localeInput(pivot, surface);
    if (inputNode == null) return null;
    assert inputNode.type() == OperatorType.INPUT;

    PlanNode pathNode = inputNode;
    boolean filtered = false, nullable = false;
    while (pathNode != surface) {
      if (pathNode.type().isFilter()) filtered = true;

      final PlanNode successor = pathNode.successor();
      if (successor.type() == LEFT_JOIN && successor.predecessors()[1] == pathNode) nullable = true;

      pathNode = successor;
    }

    return new ColumnRef(pivot.referredColumn(), filtered, nullable);
  }

  private static class ColumnRef {
    private final Column column;
    private final boolean filtered, nullable;

    private ColumnRef(Column column, boolean filtered, boolean nullable) {
      this.column = column;
      this.filtered = filtered;
      this.nullable = nullable || !column.isFlag(Flag.NOT_NULL);
    }
  }
}
