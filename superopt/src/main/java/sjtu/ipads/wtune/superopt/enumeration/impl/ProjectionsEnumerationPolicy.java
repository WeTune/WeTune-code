package sjtu.ipads.wtune.superopt.enumeration.impl;

import sjtu.ipads.wtune.superopt.relational.Projections;
import sjtu.ipads.wtune.superopt.relational.SymbolicColumns;

public class ProjectionsEnumerationPolicy extends ColumnEnumerationPolicy<Projections> {
  public static ProjectionsEnumerationPolicy create() {
    return new ProjectionsEnumerationPolicy();
  }

  @Override
  protected Iterable<SymbolicColumns> selections(SymbolicColumns source) {
    return source.selections();
  }

  @Override
  protected Projections fromSelection(SymbolicColumns columns) {
    return Projections.from(columns);
  }
}
