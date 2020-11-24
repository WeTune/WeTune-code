package sjtu.ipads.wtune.superopt.interpret.impl;

import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.interpret.Interpreter;

public class AbstractionImpl<T> implements Abstraction<T> {
  private final Interpreter interpreter;
  private String name;

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
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public Interpreter interpreter() {
    return interpreter;
  }

  @Override
  public String toString() {
    final Interpretation interpretation = interpreter.interpretation();
    final T assignment = interpretation == null ? null : interpretation.interpret(this);

    final StringBuilder builder = new StringBuilder();
    builder.append('`').append(name);
    if (assignment != null) builder.append(": ").append(assignment);
    if (interpreter.interpreterName() != null)
      builder.append("@").append(interpreter.interpreterName());
    builder.append('`');

    return builder.toString();
    //    return (interpret == null ? "`" + name + "`" : "`" + name + ": " + interpret + "`");
  }
}
