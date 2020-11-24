package sjtu.ipads.wtune.superopt.relational.impl;

import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.interpret.Interpreter;
import sjtu.ipads.wtune.superopt.relational.ConcreteColumns;
import sjtu.ipads.wtune.superopt.relational.InputSource;

import java.util.Objects;

public class NativeColumns extends MonoSourceColumns {
  private Interpreter interpreter;
  private final Abstraction<InputSource> relation;
  private final Range range = Range.ALL; // for future?
  private int minNum, maxNum; // only useful when range is SPECIFIC

  private final Abstraction<ConcreteColumns> concreteColumns;

  public NativeColumns(Interpreter interpreter, Abstraction<InputSource> source) {
    this.interpreter = interpreter;
    this.relation = source;
    this.concreteColumns = Abstraction.create(interpreter, interpreter.interpreterName() + ".c?");
  }

  @Override
  public Abstraction<InputSource> relation() {
    return relation;
  }

  @Override
  public Abstraction<ConcreteColumns> abstractions() {
    return concreteColumns;
  }

  @Override
  public void setInterpreter(Interpreter interpreter) {
    this.interpreter = interpreter;
    this.concreteColumns.setName(interpreter.interpreterName() + ".c?");
  }

  public static NativeColumns create(Interpreter interpreter, Abstraction<InputSource> source) {
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
