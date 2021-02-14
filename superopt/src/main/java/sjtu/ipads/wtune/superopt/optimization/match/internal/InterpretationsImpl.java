package sjtu.ipads.wtune.superopt.optimization.match.internal;

import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.common.multiversion.Catalog;
import sjtu.ipads.wtune.common.multiversion.Snapshot;
import sjtu.ipads.wtune.sqlparser.relational.Attribute;
import sjtu.ipads.wtune.sqlparser.schema.Column;
import sjtu.ipads.wtune.superopt.optimization.match.InputInterpretation;
import sjtu.ipads.wtune.superopt.optimization.match.Interpretations;
import sjtu.ipads.wtune.superopt.optimization.match.PredicateInterpretation;
import sjtu.ipads.wtune.superopt.optimization.match.ProjectionInterpretation;
import sjtu.ipads.wtune.superopt.plan.ConstraintRegistry;
import sjtu.ipads.wtune.superopt.plan.Placeholder;
import sjtu.ipads.wtune.superopt.optimization.*;

import java.util.ArrayList;
import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.func2;

public class InterpretationsImpl implements Interpretations {
  private Catalog<Placeholder, InputInterpretationImpl> inputs;
  private Catalog<Placeholder, ProjectionInterpretationImpl> picks;
  private ConstraintRegistry constraints;

  @Override
  public InputInterpretation interpretInput(Placeholder placeholder) {
    return null;
  }

  @Override
  public ProjectionInterpretation interpretPick(Placeholder placeholder) {
    return picks.get(placeholder);
  }

  @Override
  public PredicateInterpretation interpretPredicate(Placeholder placeholder) {
    return null;
  }

  @Override
  public boolean assignInput(Placeholder placeholder, Operator operator) {
    final InputInterpretationImpl existing = inputs.get(placeholder);
    if (existing != null) return existing.isCompatible(operator);

    final InputInterpretationImpl interpretation = new InputInterpretationImpl(operator);
    inputs.put(placeholder, interpretation);
    for (Placeholder p : constraints.eqInputOf(placeholder)) inputs.put(p, interpretation);

    for (Placeholder p : picks.keys()) if (!checkSource(p)) return false;

    return true;
  }

  @Override
  public boolean assignProjection(Placeholder placeholder, List<Attribute> colRefs) {
    final ProjectionInterpretationImpl existing = picks.get(placeholder);
    if (existing != null) return existing.isCompatible(colRefs);

    final ProjectionInterpretationImpl interpretation = new ProjectionInterpretationImpl(colRefs);
    picks.put(placeholder, interpretation);

    boolean succeed;
    if (succeed = checkSource(placeholder))
      for (Placeholder p : constraints.eqPicksOf(placeholder)) {
        picks.put(p, interpretation);
        if (!(succeed = checkSource(p))) break;
      }

    // check whether if reference constraint is violated
    return succeed && constraints.references().stream().allMatch(this::checkReference);
  }

  private boolean checkSource(Placeholder pick) {
    return false;
  }

  private boolean checkReference(Pair<Placeholder, Placeholder> pair) {
    final ProjectionInterpretationImpl referee = picks.get(pair.getLeft());
    final ProjectionInterpretationImpl referred = picks.get(pair.getRight());
    if (referee == referred) return true;

    final List<Attribute> refereeRefs = referee.projection();
    final List<Attribute> referredRefs = referred.projection();

    if (refereeRefs.size() != referredRefs.size()) return false;

    final List<Column> refereeCols = new ArrayList<>(refereeRefs.size());
    final List<Column> referredCols = new ArrayList<>(referredRefs.size());

    for (int i = 0, bound = refereeRefs.size(); i < bound; i++) {
      final Attribute refereeAttr = refereeRefs.get(i);
      final Attribute referredAttr = referredRefs.get(i);

      if (refereeAttr == null || referredAttr == null) return false;
      if (refereeAttr == referredAttr) continue;

      final Column refereeCol = refereeAttr.column(true), referredCol = referredAttr.column(true);
      if (refereeCol == referredCol) continue;

      refereeCols.add(refereeCol);
      referredCols.add(referredCol);
    }

    if (refereeCols.size() != referredCols.size()) return false;

    return refereeCols.stream().allMatch(func2(Column::references).bind1(referredCols)::apply);
  }

  @Override
  public Snapshot snapshot() {
    return inputs.snapshot().merge(picks.snapshot());
  }

  @Override
  public void setSnapshot(Snapshot snapshot) {
    inputs.setSnapshot(snapshot);
    picks.setSnapshot(snapshot);
  }

  @Override
  public void derive() {
    inputs.derive();
    picks.derive();
  }
}
