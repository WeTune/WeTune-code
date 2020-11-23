package sjtu.ipads.wtune.superopt.relational.impl;

import sjtu.ipads.wtune.superopt.relational.Projections;
import sjtu.ipads.wtune.superopt.relational.SymbolicColumns;

import java.util.Objects;

// Currently this is just a wrapper of SymbolicColumn
public class ProjectionsImpl implements Projections {
  private final SymbolicColumns columns;

  private ProjectionsImpl(SymbolicColumns columns) {
    this.columns = columns;
  }

  public static ProjectionsImpl create(SymbolicColumns columns) {
    return new ProjectionsImpl(columns);
  }

  @Override
  public SymbolicColumns columns() {
    return columns;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ProjectionsImpl that = (ProjectionsImpl) o;
    return Objects.equals(columns, that.columns);
  }

  @Override
  public int hashCode() {
    return Objects.hash(columns);
  }

  @Override
  public String toString() {
    return "Projections(" + columns + ')';
  }
}
