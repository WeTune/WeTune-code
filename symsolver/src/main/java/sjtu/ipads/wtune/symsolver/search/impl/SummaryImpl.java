package sjtu.ipads.wtune.symsolver.search.impl;

import sjtu.ipads.wtune.symsolver.DecidableConstraint;
import sjtu.ipads.wtune.symsolver.core.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import static sjtu.ipads.wtune.common.utils.Commons.isSubSequence;
import static sjtu.ipads.wtune.common.utils.Commons.sorted;

final class SummaryImpl implements Summary {
  private final TableSym[] tables;
  private final PickSym[] picks;
  private final PredicateSym[] preds;
  private final DecidableConstraint[] constraints;

  private final TracerImpl tracer;
  private final int expectedModCount;

  Collection<Collection<TableSym>> tableGroups;
  Collection<Collection<PickSym>> pickGroups;
  Collection<Collection<PredicateSym>> predGroups;
  TableSym[][] pivotedSources;
  Map<PickSym, PickSym> references;

  SummaryImpl(
      TableSym[] tables,
      PickSym[] picks,
      PredicateSym[] preds,
      DecidableConstraint[] constraints,
      TracerImpl tracer) {
    this.tables = tables;
    this.picks = picks;
    this.preds = preds;
    this.constraints = sorted(constraints, DecidableConstraint::compareTo);
    this.tracer = tracer;
    this.expectedModCount = tracer.modCount();
  }

  @Override
  public Constraint[] constraints() {
    return constraints;
  }

  @Override
  public Collection<Collection<TableSym>> tableGroups() {
    inflate();
    return tableGroups;
  }

  @Override
  public Collection<Collection<PickSym>> pickGroups() {
    inflate();
    return pickGroups;
  }

  @Override
  public Collection<Collection<PredicateSym>> predicateGroups() {
    return predGroups;
  }

  @Override
  public TableSym[][] pivotedSources() {
    inflate();
    return pivotedSources;
  }

  @Override
  public Map<PickSym, PickSym> references() {
    inflate();
    return references;
  }

  @Override
  public boolean implies(Summary other) {
    return isSubSequence(constraints, other.constraints());
  }

  private void inflate() {
    if (tableGroups != null) return;

    final TracerImpl helper;
    if (tracer.modCount() == expectedModCount) helper = tracer;
    else {
      helper = (TracerImpl) TracerImpl.build(tables, picks, preds);
      helper.decide(constraints);
    }

    helper.inflateSummary(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SummaryImpl summary = (SummaryImpl) o;
    return Arrays.equals(constraints, summary.constraints);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(constraints);
  }

  @Override
  public String toString() {
    return "tables: "
        + tableGroups()
        + " picks: "
        + pickGroups()
        + " predicates: "
        + predicateGroups()
        + " srcs: "
        + Arrays.deepToString(pivotedSources())
        + " refs: "
        + references();
  }
}
