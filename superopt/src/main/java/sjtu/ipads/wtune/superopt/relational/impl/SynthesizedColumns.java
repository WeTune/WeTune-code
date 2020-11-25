package sjtu.ipads.wtune.superopt.relational.impl;

import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.interpret.Interpreter;
import sjtu.ipads.wtune.superopt.relational.ColumnSet;
import sjtu.ipads.wtune.superopt.relational.ConcreteColumns;
import sjtu.ipads.wtune.superopt.relational.MonoSourceColumnSet;

import java.util.List;

import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.superopt.constraint.Constraint.contradiction;

public class SynthesizedColumns implements MonoSourceColumnSet, Interpreter {
  private final ColumnSet[] refSources;
  private final Interpreter interpreter;
  private Abstraction<ConcreteColumns> concreteColumns;

  private int id;

  private SynthesizedColumns(Interpreter interpreter, ColumnSet[] refSources) {
    this.interpreter = interpreter;
    this.refSources = refSources;
  }

  public static SynthesizedColumns from(Interpreter interpreter, ColumnSet[] columns) {
    return new SynthesizedColumns(interpreter, columns);
  }

  @Override
  public List<List<Constraint>> enforceEq(ColumnSet columnSet, Interpretation interpretation) {
    return singletonList(singletonList(contradiction())); // TODO
  }

  @Override
  public Abstraction<ConcreteColumns> abstractions() {
    if (concreteColumns == null) concreteColumns = Abstraction.create(this, "");
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
  public MonoSourceColumnSet copy() {
    return new SynthesizedColumns(concreteColumns.interpreter(), refSources);
  }

  @Override
  public void setInterpreter(Interpreter interpreter) {}

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
