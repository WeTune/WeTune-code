package sjtu.ipads.wtune.superopt.relational.impl;

import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.relational.Relation;
import sjtu.ipads.wtune.superopt.relational.SymbolicColumns;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public class SingleSourceSymbolicColumns implements SymbolicColumns {
  private final Abstraction<Relation> relation;
  private final Range range = Range.ALL; // for future?
  private int minNum, maxNum; // only useful when range is SPECIFIC

  public SingleSourceSymbolicColumns(Abstraction<Relation> source) {
    this.relation = source;
  }

  @Override
  public SymbolicColumns concat(SymbolicColumns other) {
    return MultiSourceSymbolicColumns.empty().concat(this).concat(other);
  }

  @Override
  public Set<SymbolicColumns> selections(int max) {
    return Collections.singleton(this);
  }

  public static SingleSourceSymbolicColumns create(Abstraction<Relation> source) {
    return new SingleSourceSymbolicColumns(source);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SingleSourceSymbolicColumns that = (SingleSourceSymbolicColumns) o;
    return Objects.equals(relation, that.relation);
  }

  @Override
  public int hashCode() {
    return Objects.hash(relation);
  }

  @Override
  public String toString() {
    return relation + ".?";
  }
}
