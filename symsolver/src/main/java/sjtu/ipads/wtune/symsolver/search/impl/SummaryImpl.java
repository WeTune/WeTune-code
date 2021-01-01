package sjtu.ipads.wtune.symsolver.search.impl;

import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.TableSym;
import sjtu.ipads.wtune.symsolver.search.Summary;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import static sjtu.ipads.wtune.common.utils.FuncUtils.isSubSequence;
import static sjtu.ipads.wtune.common.utils.FuncUtils.sorted;

final class SummaryImpl implements Summary {
  private final TableSym[] tables;
  private final PickSym[] picks;
  private final Constraint[] constraints;

  private final TracerImpl tracer;
  private final int expectedModCount;

  Collection<Collection<TableSym>> tableGroups;
  Collection<Collection<PickSym>> pickGroups;
  TableSym[][] pivotedSources;
  Map<PickSym, PickSym> references;

  SummaryImpl(TableSym[] tables, PickSym[] picks, Constraint[] constraints, TracerImpl tracer) {
    this.tables = tables;
    this.picks = picks;
    this.constraints = sorted(constraints, Constraint::compareTo);
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
      helper = (TracerImpl) TracerImpl.build(tables, picks);
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
        + " srcs: "
        + Arrays.deepToString(pivotedSources())
        + " refs: "
        + references();
  }
}
