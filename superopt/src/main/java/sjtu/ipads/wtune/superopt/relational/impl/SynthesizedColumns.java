package sjtu.ipads.wtune.superopt.relational.impl;

import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.interpret.Interpreter;
import sjtu.ipads.wtune.superopt.relational.ConcreteColumns;
import sjtu.ipads.wtune.superopt.relational.SymbolicColumns;

public class SynthesizedColumns extends MonoSourceColumns {
  private final SymbolicColumns[] refSources;
  private final Abstraction<ConcreteColumns> concreteColumns;

  private SynthesizedColumns(Interpreter interpreter, SymbolicColumns[] refSources) {
    this.refSources = refSources;
    this.concreteColumns = Abstraction.create(interpreter, "c?");
  }

  public static SynthesizedColumns from(Interpreter interpreter, SymbolicColumns[] columns) {
    return new SynthesizedColumns(interpreter, columns);
  }

  @Override
  public MonoSourceColumns copy() {
    return new SynthesizedColumns(concreteColumns.interpreter(), refSources);
  }

  @Override
  public void setInterpreter(Interpreter interpreter) {}
}
