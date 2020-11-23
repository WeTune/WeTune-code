package sjtu.ipads.wtune.superopt.interpret.impl;

import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.interpret.Interpreter;

public class AbstractionImpl<T> implements Abstraction<T> {
  private final Interpreter interpreter;
  private final String name;

  private AbstractionImpl(Interpreter interpreter, String name) {
    this.interpreter = interpreter;
    this.name = name;
  }

  public static <T> AbstractionImpl<T> create(Interpreter interpreter, String name) {
    return new AbstractionImpl<>(interpreter, name == null ? "anony" : name);
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public Interpreter interpreter() {
    return interpreter;
  }

  @Override
  public String toString() {
    final Interpretation interpretation = interpreter.interpretation();
    final T interpret = interpretation == null ? null : interpretation.interpret(this);

    return (interpreter.interpreterName() == null ? "" : interpreter.interpreterName() + ".")
        + (interpret == null ? "`" + name + "`" : "`" + name + ": " + interpret + "`");
    //    return (interpret == null ? "`" + name + "`" : "`" + name + ": " + interpret + "`");
  }
}
