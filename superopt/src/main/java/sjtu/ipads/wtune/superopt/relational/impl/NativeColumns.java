package sjtu.ipads.wtune.superopt.relational.impl;

import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.interpret.Interpreter;
import sjtu.ipads.wtune.superopt.relational.ConcreteColumns;
import sjtu.ipads.wtune.superopt.relational.Relation;

import java.util.Objects;

public class NativeColumns extends MonoSourceColumns {
  private final Abstraction<Relation> relation;
  private final Range range = Range.ALL; // for future?
  private int minNum, maxNum; // only useful when range is SPECIFIC

  private final Abstraction<ConcreteColumns> concreteColumns;

  public NativeColumns(Interpreter interpreter, Abstraction<Relation> source) {
    this.relation = source;
    this.concreteColumns = Abstraction.create(interpreter, source.name() + ".c?");
  }

  @Override
  public Abstraction<Relation> relation() {
    return relation;
  }

  @Override
  public Abstraction<ConcreteColumns> abstractions() {
    return concreteColumns;
  }

  public static NativeColumns create(Interpreter interpreter, Abstraction<Relation> source) {
    return new NativeColumns(interpreter, source);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    NativeColumns that = (NativeColumns) o;
    return Objects.equals(relation, that.relation);
  }

  @Override
  public int hashCode() {
    return Objects.hash(relation);
  }

  @Override
  public String toString() {
    return relation + "." + concreteColumns.name();
  }

  @Override
  public MonoSourceColumns copy() {
    return new NativeColumns(concreteColumns.interpreter(), relation);
  }
}
