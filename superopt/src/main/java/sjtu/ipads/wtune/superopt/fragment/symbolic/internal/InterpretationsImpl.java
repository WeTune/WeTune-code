package sjtu.ipads.wtune.superopt.fragment.symbolic.internal;

import sjtu.ipads.wtune.common.multiversion.Catalog;
import sjtu.ipads.wtune.common.multiversion.CatalogBase;
import sjtu.ipads.wtune.common.multiversion.Snapshot;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.OutputAttribute;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.schema.Column;
import sjtu.ipads.wtune.superopt.fragment.symbolic.*;
import sjtu.ipads.wtune.superopt.util.Constraints;
import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.PickFrom;
import sjtu.ipads.wtune.symsolver.core.Reference;

import java.util.ArrayList;
import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.stream;

public class InterpretationsImpl implements Interpretations {
  private final Constraints constraints;
  private final Catalog<Placeholder, Interpretation> interpretations;

  private InterpretationsImpl(Constraints constraints) {
    this.constraints = constraints;
    this.interpretations = new CatalogBase<>();
  }

  public static Interpretations build(Constraints constraints) {
    return new InterpretationsImpl(constraints);
  }

  @Override
  public Constraints constraints() {
    return constraints;
  }

  @Override
  public InputInterpretation getInput(Placeholder placeholder) {
    return ((InputInterpretation) interpretations.get(placeholder));
  }

  @Override
  public AttributeInterpretation getAttributes(Placeholder placeholder) {
    return ((AttributeInterpretation) interpretations.get(placeholder));
  }

  @Override
  public PredicateInterpretation getPredicate(Placeholder placeholder) {
    return ((PredicateInterpretation) interpretations.get(placeholder));
  }

  @Override
  public boolean assignInput(Placeholder placeholder, PlanNode node) {
    return assign0(placeholder, new InputInterpretationImpl(node));
  }

  @Override
  public boolean assignAttributes(Placeholder placeholder, List<OutputAttribute> colRefs) {
    return assign0(placeholder, new AttributeInterpretationImpl(colRefs));
  }

  @Override
  public boolean assignPredicate(Placeholder placeholder, ASTNode expr) {
    return assign0(placeholder, new PredicateInterpretationImpl(expr));
  }

  private <T> boolean assign0(Placeholder placeholder, Interpretation<T> assignment) {
    if (!assign1(placeholder, assignment)) return false;
    for (Placeholder p : constraints.equivalenceOf(placeholder))
      if (!assign1(p, assignment)) return false;

    return checkConstraints();
  }

  private <T> boolean assign1(Placeholder placeholder, Interpretation<T> assignment) {
    final Interpretation<T> existingAssignment =
        (Interpretation<T>) interpretations.get(placeholder);
    if (existingAssignment != null) return existingAssignment.isCompatible(assignment);
    interpretations.put(placeholder, assignment);
    return true;
  }

  @Override
  public Snapshot snapshot() {
    return interpretations.snapshot();
  }

  @Override
  public void setSnapshot(Snapshot snapshot) {
    interpretations.setSnapshot(snapshot);
  }

  @Override
  public void derive() {
    interpretations.derive();
  }

  private boolean checkConstraints() {
    return stream(constraints).allMatch(this::checkConstraint);
  }

  private boolean checkConstraint(Constraint constraint) {
    if (constraint.kind() == Constraint.Kind.Reference)
      return checkReference((Reference<Placeholder, Placeholder>) constraint);
    else if (constraint.kind() == Constraint.Kind.PickFrom)
      return checkSource((PickFrom<Placeholder, Placeholder>) constraint);
    else return true;
  }

  private boolean checkSource(PickFrom<Placeholder, Placeholder> constraint) {
    final AttributeInterpretation attrInter = getAttributes(constraint.p());
    if (attrInter == null) return true;
    final List<OutputAttribute> attributes = attrInter.object();

    final Placeholder[] ts = constraint.ts();
    final PlanNode[] inputs = new PlanNode[ts.length];
    for (int i = 0; i < ts.length; i++) {
      final InputInterpretation inter = getInput(ts[i]);
      if (inter == null) return true;
      inputs[i] = inter.object();
    }

    for (OutputAttribute attribute : attributes)
      if (stream(inputs).noneMatch(it -> it.resolveAttribute(attribute) != null)) return false;

    return true;
  }

  private boolean checkReference(Reference<Placeholder, Placeholder> constraint) {
    final AttributeInterpretation referee = getAttributes(constraint.px());
    final AttributeInterpretation referred = getAttributes(constraint.py());

    if (referee == null || referred == null || referee == referred) return true;

    final List<OutputAttribute> refereeRefs = referee.object();
    final List<OutputAttribute> referredRefs = referred.object();

    if (refereeRefs.size() != referredRefs.size()) return false;

    final List<Column> refereeCols = new ArrayList<>(refereeRefs.size());
    final List<Column> referredCols = new ArrayList<>(referredRefs.size());

    for (int i = 0, bound = refereeRefs.size(); i < bound; i++) {
      final OutputAttribute refereeAttr = refereeRefs.get(i);
      final OutputAttribute referredAttr = referredRefs.get(i);

      if (refereeAttr == null || referredAttr == null) return false;
      if (refereeAttr == referredAttr) continue;

      final Column refereeCol = refereeAttr.column(true), referredCol = referredAttr.column(true);
      if (refereeCol == referredCol) continue;

      refereeCols.add(refereeCol);
      referredCols.add(referredCol);
    }

    if (refereeCols.size() != referredCols.size()) return false;

    for (Column refereeCol : refereeCols) if (!refereeCol.references(referredCols)) return false;
    return true;
  }
}
