package sjtu.ipads.wtune.superopt.relational.impl;

import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.interpret.Interpreter;
import sjtu.ipads.wtune.superopt.relational.ColumnSet;
import sjtu.ipads.wtune.superopt.relational.ConcreteColumns;
import sjtu.ipads.wtune.superopt.relational.InputSource;
import sjtu.ipads.wtune.superopt.relational.MonoSourceColumnSet;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.superopt.constraint.Constraint.contradiction;

public class NativeColumns implements MonoSourceColumnSet, Interpreter {
  private Interpreter interpreter;
  private final Abstraction<InputSource> source;
  private final Range range = Range.ALL; // for future?
  private int minNum, maxNum; // only useful when range is SPECIFIC

  private int id = -1; // only for display

  private Abstraction<ConcreteColumns> concreteColumns;

  private NativeColumns(Interpreter interpreter, Abstraction<InputSource> source) {
    this.interpreter = interpreter;
    this.source = source;
  }

  @Override
  public Abstraction<InputSource> source() {
    return source;
  }

  @Override
  public Abstraction<ConcreteColumns> abstractions() {
    if (this.concreteColumns == null) concreteColumns = Abstraction.create(this, "");
    return concreteColumns;
  }

  @Override
  public int id() {
    return id;
  }

  @Override
  public void setId(int id) {
    this.id = id;
  }

  @Override
  public void setInterpreter(Interpreter interpreter) {
    this.interpreter = interpreter;
  }

  public static NativeColumns create(Interpreter interpreter, Abstraction<InputSource> source) {
    return new NativeColumns(interpreter, source);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    NativeColumns that = (NativeColumns) o;
    return Objects.equals(source, that.source);
  }

  @Override
  public int hashCode() {
    return Objects.hash(source);
  }

  @Override
  public String toString() {
    return source + ".c" + (id == -1 ? "?" : id);
  }

  @Override
  public MonoSourceColumnSet copy() {
    return new NativeColumns(interpreter, source);
  }

  @Override
  public List<List<Constraint>> enforceEq(ColumnSet other, Interpretation interpretation) {
    final Set<MonoSourceColumnSet> flatten = other.flatten();
    if (flatten.size() > 1) return singletonList(singletonList(contradiction()));
    final MonoSourceColumnSet otherSingleCol = flatten.iterator().next();

    return singletonList(
        List.of(
            Constraint.refEq(source(), otherSingleCol.source()),
            Constraint.refEq(abstractions(), otherSingleCol.abstractions())));
  }

  @Override
  public Interpretation interpretation() {
    return interpreter.interpretation();
  }

  @Override
  public String interpreterName() {
    return toString();
  }

  @Override
  public void setInterpretation(Interpretation interpretation) {
    interpreter.setInterpretation(interpretation);
  }
}
