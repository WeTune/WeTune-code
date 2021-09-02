package sjtu.ipads.wtune.superopt.fragment;

import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.sqlparser.ast.constants.ConstraintType;
import sjtu.ipads.wtune.sqlparser.plan.*;
import sjtu.ipads.wtune.sqlparser.schema.Column;
import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.constraint.Constraints;

import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.function.Predicate.not;
import static sjtu.ipads.wtune.common.utils.FuncUtils.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ConstraintType.UNIQUE;
import static sjtu.ipads.wtune.sqlparser.plan.ValueBag.locateValueRelaxed;
import static sjtu.ipads.wtune.sqlparser.schema.SchemaSupport.findIC;
import static sjtu.ipads.wtune.sqlparser.schema.SchemaSupport.findRelatedIC;

class ConstraintAwareModelImpl extends ModelImpl implements ConstraintAwareModel {
  private final PlanContext plan;
  private final Constraints constraints;
  private final Set<Symbol> trustedAssigned;
  private ListMultimap<Symbol, List<Value>> outAttrsAssignments;

  ConstraintAwareModelImpl(PlanContext plan, Constraints constraints) {
    this(null, plan, constraints);
  }

  ConstraintAwareModelImpl(
      ConstraintAwareModelImpl base, PlanContext plan, Constraints constraints) {
    super(base);
    this.plan = plan;
    this.constraints = constraints;
    this.trustedAssigned = new HashSet<>(16);
  }

  @Override
  public Constraints constraints() {
    return constraints;
  }

  @Override
  public void reset() {
    assignments.clear();
    trustedAssigned.clear();
  }

  @Override
  public ConstraintAwareModel base() {
    return (ConstraintAwareModel) base;
  }

  @Override
  public ConstraintAwareModel derive() {
    return new ConstraintAwareModelImpl(this, plan, constraints);
  }

  @Override
  public ConstraintAwareModel derive(PlanContext ctx) {
    return new ConstraintAwareModelImpl(this, ctx, constraints);
  }

  @Override
  public PlanContext planContext() {
    return plan;
  }

  @Override
  public List<Value> interpretInAttrs(Symbol attrs) {
    return get0(attrs);
  }

  @Override
  public List<List<Value>> interpretOutAttrs(Symbol attrs) {
    final List<List<Value>> v = outAttrsAssignments == null ? null : outAttrsAssignments.get(attrs);
    return v != null ? v : base == null ? emptyList() : base.interpretOutAttrs(attrs);
  }

  @Override
  public boolean isAssignmentTrusted(Symbol sym) {
    return trustedAssigned.contains(sym) || (base != null && base.isAssignmentTrusted(sym));
  }

  @Override
  protected boolean assign0(Symbol target, Object newVal) {
    List<Value> outAttrs = null;
    if (newVal instanceof Pair) {
      outAttrs = ((Pair<?, List<Value>>) newVal).getRight();
      newVal = ((Pair<?, ?>) newVal).getLeft();
    }

    for (Symbol eqSym : constraints.eqClassOf(target)) {
      final Object oldVal = get0(eqSym);
      if (oldVal != null && !checkCompatible(newVal, oldVal)) return false;
      boolean trusted = target == eqSym;
      if (oldVal == null || trusted) super.assign0(eqSym, newVal);
      if (trusted) trustedAssigned.add(eqSym);
      if (outAttrs != null) addOutAttrs(eqSym, outAttrs, trusted);
    }
    return true;
  }

  @Override
  public boolean checkConstraint(boolean strict) {
    final Set<Symbol> assignedSymbols = assignments.keySet();
    for (Constraint constraint : constraints)
      if (strict || any(asList(constraint.symbols()), assignedSymbols::contains))
        if (!checkConstraint(constraint, strict)) {
          return false;
        }

    return true;
  }

  private boolean checkCompatible(Object newOne, Object oldOne) {
    if (oldOne == newOne) {
      return true;

    } else if (oldOne instanceof List) {
      return isCompatibleValues((List<Value>) oldOne, (List<Value>) newOne);

    } else if (oldOne instanceof InputNode) {
      return newOne instanceof InputNode
          && ((InputNode) oldOne).table().equals(((InputNode) newOne).table());

    } else {
      return oldOne.toString().equals(newOne.toString());
    }
  }

  private boolean checkConstraint(Constraint constraint, boolean strict) {
    switch (constraint.kind()) {
      case AttrsSub:
        return checkAttrSub(constraint, strict);
      case Unique:
        return checkUnique(constraint);
      case Reference:
        return checkReference(constraint);
      default:
        return true;
    }
  }

  private boolean checkAttrSub(Constraint attrsSub, boolean strict) {
    // "strict" mode
    // Some rules have only AttrsSub on the RHS. e.g. ...|Proj<a3>(Proj<a4>(...))|AttrsSub(a3,a4)
    // If a4 is not assigned with out-values, then the AttrsSub cannot be checked.
    // So, in "strict" we will check against a4's in-values instead.
    assert attrsSub.kind() == Constraint.Kind.AttrsSub;

    final Symbol attrsSym = attrsSub.symbols()[0];
    final Symbol srcSym = attrsSub.symbols()[1];

    final List<Value> attrs = interpretInAttrs(attrsSym);
    if (attrs == null) return true;

    final List<Value> srcAttrs;
    if (srcSym.kind() == Symbol.Kind.TABLE) {
      final PlanNode inputNode = interpretTable(srcSym);
      if (inputNode == null) return true;
      srcAttrs = inputNode.values();
    } else {
      final List<Value> tmp = getOutAttrs(srcSym);
      if (tmp != null) srcAttrs = tmp;
      else if (!strict) return true;
      else srcAttrs = interpretInAttrs(srcSym);
    }

    final boolean trusted = isAssignmentTrusted(attrsSym) && isAssignmentTrusted(srcSym);
    if (trusted) return srcAttrs.containsAll(attrs);
    else return all(attrs, attr -> locateValueRelaxed(srcAttrs, attr, plan) != null);
  }

  private boolean checkUnique(Constraint unique) {
    assert unique.kind() == Constraint.Kind.Unique;

    final Symbol srcSym = unique.symbols()[0];
    final Symbol attrsSym = unique.symbols()[1];

    final List<Value> attrs = interpretInAttrs(attrsSym);
    if (attrs == null) return true;

    final List<Column> columns = resolveSourceColumns(attrs);
    if (columns == null) return false;

    for (Column column : columns)
      if (Iterables.isEmpty(findRelatedIC(plan.schema(), column, UNIQUE))) {
        return false;
      }

    if (!isAssignmentTrusted(srcSym) || !isAssignmentTrusted(attrsSym)) return true;

    final PlanNode surface = interpretTable(srcSym);
    if (surface == null) return true;

    return isUniqueCoreOf(new HashSet<>(attrs), surface);
  }

  private boolean checkReference(Constraint reference) {
    assert reference.kind() == Constraint.Kind.Reference;

    final Symbol referringAttrsSym = reference.symbols()[1];
    final Symbol referredSrcSym = reference.symbols()[2];
    final Symbol referredAttrsSym = reference.symbols()[3];

    final List<Value> referringAttrs = interpretInAttrs(referringAttrsSym);
    if (referringAttrs == null) return true;

    final List<Column> referringColumns = resolveSourceColumns(referringAttrs);
    if (referringColumns == null) return false;

    // Check integrity constraint on schema.
    final var fk = findIC(plan.schema(), referringColumns, ConstraintType.FOREIGN);
    if (fk == null) return false;

    final List<Value> referredAttrs = interpretInAttrs(referredAttrsSym);
    if (referredAttrs == null) return true;
    if (!fk.refColumns().equals(resolveSourceColumns(referredAttrs))) return false;

    // Check if the referred attributes are filtered, which invalidates the FK on schema.
    final PlanNode surface = interpretTable(referredSrcSym);
    if (surface == null) return true;
    if (!isAssignmentTrusted(referredAttrsSym) || !isAssignmentTrusted(referredSrcSym)) return true;

    final PlanNode inputNode = plan.ownerOf(plan.sourceOf(referredAttrs.get(0)));
    assert inputNode.kind() == OperatorType.INPUT;

    PlanNode path = inputNode;
    while (path != surface) {
      if (path.successor().predecessors()[0] == path && path.successor().kind().isFilter())
        return false;
      path = path.successor();
    }

    return true;
  }

  private boolean isCompatibleValues(List<Value> values0, List<Value> values1) {
    if (values0.size() != values1.size()) return false;
    for (int i = 0, bound = values0.size(); i < bound; i++)
      if (locateValueRelaxed(singletonList(values0.get(i)), values1.get(i), plan) == null) {
        return false;
      }
    return true;
  }

  private List<Value> getOutAttrs(Symbol sym) {
    if (outAttrsAssignments == null) return null;
    final List<List<Value>> lists = outAttrsAssignments.get(sym);
    return lists.isEmpty() ? null : lists.get(0);
  }

  private void addOutAttrs(Symbol sym, List<Value> values, boolean trusted) {
    if (outAttrsAssignments == null)
      outAttrsAssignments = MultimapBuilder.hashKeys(2).arrayListValues(2).build();
    final List<List<Value>> lists = outAttrsAssignments.get(sym);
    if (trusted) lists.add(0, values);
    else lists.add(values);
  }

  private List<Column> resolveSourceColumns(List<Value> attrs) {
    final List<Column> columns = new ArrayList<>(attrs.size());

    for (Value attr : attrs) {
      final Value srcAttr = plan.sourceOf(attr);
      if (srcAttr.column() == null) return null; // conservative reject
      else columns.add(srcAttr.column());
    }

    return columns;
  }

  private boolean isUniqueCoreOf(Set<Value> attrs, PlanNode surface) {
    final OperatorType kind = surface.kind();

    if (kind.isJoin()) {
      final JoinNode join = (JoinNode) surface;
      final ValueBag lhsValues = join.predecessors()[0].values();
      final Set<Value> lhs = setFilter(attrs, lhsValues::contains);
      final Set<Value> rhs = setFilter(attrs, not(lhs::contains));

      if (join.isEquiJoin()) {
        final List<Value> lhsJoinAttrs = plan.deRef(join.lhsRefs());
        final List<Value> rhsJoinAttrs = plan.deRef(join.rhsRefs());
        for (int i = 0, bound = lhsJoinAttrs.size(); i < bound; i++) {
          final Value lhsJoinAttr = lhsJoinAttrs.get(i);
          final Value rhsJoinAttr = rhsJoinAttrs.get(i);
          if (lhs.contains(lhsJoinAttr)) rhs.add(rhsJoinAttr);
          if (rhs.contains(rhsJoinAttr)) lhs.add(lhsJoinAttr);
        }
      }

      return isUniqueCoreOf(rhs, surface.predecessors()[1])
          && isUniqueCoreOf(lhs, surface.predecessors()[0]);

    } else if (kind == OperatorType.PROJ) {
      final Set<Value> srcAttrs = setMap(attrs, it -> plan.deRef(it.expr().refs().get(0)));
      return isUniqueCoreOf(srcAttrs, surface.predecessors()[0]);

    } else if (kind == OperatorType.SIMPLE_FILTER) {
      final SimpleFilterNode filter = (SimpleFilterNode) surface;
      if (filter.predicate().isEquiCondition()) {
        final Value fixedAttr = plan.deRef(filter.refs().get(0));
        attrs.add(fixedAttr);
      }
      return isUniqueCoreOf(attrs, surface.predecessors()[0]);

    } else if (kind == OperatorType.INPUT) {
      final InputNode inputNode = (InputNode) surface;
      final Set<Column> columns = setMap(attrs, Value::column);
      assert none(columns, Objects::isNull);

      return any(inputNode.table().constraints(UNIQUE), it -> columns.containsAll(it.columns()));

    } else {
      return isUniqueCoreOf(attrs, surface.predecessors()[0]);
    }
  }
}
