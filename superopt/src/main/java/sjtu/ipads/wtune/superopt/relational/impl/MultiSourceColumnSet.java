package sjtu.ipads.wtune.superopt.relational.impl;

import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.interpret.Interpreter;
import sjtu.ipads.wtune.superopt.relational.ColumnSet;
import sjtu.ipads.wtune.superopt.relational.MonoSourceColumnSet;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.superopt.constraint.Constraint.contradiction;

public class MultiSourceColumnSet implements ColumnSet {
  private final Set<MonoSourceColumnSet> singleColumns;

  private MultiSourceColumnSet() {
    singleColumns = new HashSet<>();
  }

  private MultiSourceColumnSet(Set<MonoSourceColumnSet> columns) {
    singleColumns = columns;
  }

  public static MultiSourceColumnSet from(Set<MonoSourceColumnSet> columns) {
    return new MultiSourceColumnSet(columns);
  }

  public static MultiSourceColumnSet copyFrom(Set<MonoSourceColumnSet> columns) {
    return new MultiSourceColumnSet(
        columns.stream().map(MonoSourceColumnSet::copy).collect(Collectors.toSet()));
  }

  @Override
  public List<List<Constraint>> enforceEq(ColumnSet other, Interpretation interpretation) {
    if (other instanceof MonoSourceColumnSet) return other.enforceEq(this, interpretation);
    final Set<MonoSourceColumnSet> thisSet = flatten();
    final Set<MonoSourceColumnSet> otherSet = other.flatten();
    if (thisSet.size() != otherSet.size()) return singletonList(singletonList(contradiction()));

    if (thisSet.size() == 1) {
      final MonoSourceColumnSet thisSingle = thisSet.iterator().next();
      return thisSingle.enforceEq(other, interpretation);
    } else {
      return singletonList(singletonList(contradiction())); // TODO
    }
  }

  @Override
  public void setInterpreter(Interpreter interpreter) {
    singleColumns.forEach(it -> it.setInterpreter(interpreter));
  }

  @Override
  public Set<MonoSourceColumnSet> flatten() {
    return singleColumns;
  }

  @Override
  public ColumnSet copy() {
    return copyFrom(singleColumns);
  }

  @Override
  public ColumnSet union(ColumnSet other) {
    return new MultiSourceColumnSet().union0(this).union0(other);
  }

  private MultiSourceColumnSet union0(ColumnSet other) {
    if (other instanceof MonoSourceColumnSet) singleColumns.add((MonoSourceColumnSet) other);
    else if (other instanceof MultiSourceColumnSet)
      singleColumns.addAll(((MultiSourceColumnSet) other).singleColumns);
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MultiSourceColumnSet that = (MultiSourceColumnSet) o;
    return Objects.equals(singleColumns, that.singleColumns);
  }

  @Override
  public int hashCode() {
    return Objects.hash(singleColumns);
  }

  @Override
  public String toString() {
    return singleColumns.stream().map(Object::toString).collect(Collectors.joining(", "));
  }
}
