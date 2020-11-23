package sjtu.ipads.wtune.superopt.relational.impl;

import com.google.common.collect.Sets;
import sjtu.ipads.wtune.superopt.relational.SymbolicColumns;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class MultiSourceSymbolicColumns implements SymbolicColumns {
  private final Set<SingleSourceSymbolicColumns> singleColumns;
  private Set<SymbolicColumns> selections;

  private MultiSourceSymbolicColumns() {
    singleColumns = new HashSet<>();
  }

  private MultiSourceSymbolicColumns(Set<SingleSourceSymbolicColumns> columns) {
    singleColumns = columns;
  }

  public static MultiSourceSymbolicColumns empty() {
    return new MultiSourceSymbolicColumns();
  }

  public static MultiSourceSymbolicColumns from(Set<SingleSourceSymbolicColumns> columns) {
    return new MultiSourceSymbolicColumns(columns);
  }

  @Override
  public SymbolicColumns concat(SymbolicColumns other) {
    return new MultiSourceSymbolicColumns().concat0(this).concat0(other);
  }

  @Override
  public Set<SymbolicColumns> selections(int max) {
    if (selections != null) return selections;

    final Set<SymbolicColumns> selections = new HashSet<>();
    for (Set<SingleSourceSymbolicColumns> columns : Sets.powerSet(singleColumns))
      if (!columns.isEmpty() && columns.size() <= max) selections.add(from(columns));

    this.selections = selections;

    return selections;
  }

  private MultiSourceSymbolicColumns concat0(SymbolicColumns other) {
    if (other instanceof SingleSourceSymbolicColumns)
      singleColumns.add((SingleSourceSymbolicColumns) other);
    else if (other instanceof MultiSourceSymbolicColumns)
      singleColumns.addAll(((MultiSourceSymbolicColumns) other).singleColumns);
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MultiSourceSymbolicColumns that = (MultiSourceSymbolicColumns) o;
    return Objects.equals(singleColumns, that.singleColumns);
  }

  @Override
  public int hashCode() {
    return Objects.hash(singleColumns);
  }

  @Override
  public String toString() {
    return singleColumns.toString();
  }
}
