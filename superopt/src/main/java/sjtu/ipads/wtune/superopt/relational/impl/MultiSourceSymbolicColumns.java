package sjtu.ipads.wtune.superopt.relational.impl;

import com.google.common.collect.Sets;
import sjtu.ipads.wtune.superopt.interpret.Interpreter;
import sjtu.ipads.wtune.superopt.relational.SymbolicColumns;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class MultiSourceSymbolicColumns implements SymbolicColumns {
  private final Set<MonoSourceColumns> singleColumns;

  private MultiSourceSymbolicColumns() {
    singleColumns = new HashSet<>();
  }

  private MultiSourceSymbolicColumns(Set<MonoSourceColumns> columns) {
    singleColumns = columns;
  }

  public static MultiSourceSymbolicColumns empty() {
    return new MultiSourceSymbolicColumns();
  }

  private static MultiSourceSymbolicColumns copyFrom(Set<MonoSourceColumns> columns) {
    return new MultiSourceSymbolicColumns(
        columns.stream().map(MonoSourceColumns::copy).collect(Collectors.toSet()));
  }

  @Override
  public void setInterpreter(Interpreter interpreter) {
    singleColumns.forEach(it -> it.setInterpreter(interpreter));
  }

  @Override
  public Set<? extends SymbolicColumns> flatten() {
    return singleColumns;
  }

  @Override
  public SymbolicColumns copy() {
    return copyFrom(singleColumns);
  }

  @Override
  public SymbolicColumns concat(SymbolicColumns other) {
    return new MultiSourceSymbolicColumns().concat0(this).concat0(other);
  }

  @Override
  public Set<SymbolicColumns> selections(int max) {
    final Set<SymbolicColumns> selections = new HashSet<>();
    for (Set<MonoSourceColumns> columns : Sets.powerSet(singleColumns))
      if (!columns.isEmpty() && columns.size() <= max) selections.add(copyFrom(columns));

    return selections;
  }

  MultiSourceSymbolicColumns concat0(SymbolicColumns other) {
    if (other instanceof MonoSourceColumns) singleColumns.add((MonoSourceColumns) other);
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
