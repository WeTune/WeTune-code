package sjtu.ipads.wtune.superopt.interpret.impl;

import sjtu.ipads.wtune.superopt.interpret.Abstraction;
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
    final StringBuilder builder = new StringBuilder();
    if (interpreter.interpreterName() != null) {
      builder.append(interpreter.interpreterName());
      if (!name.isEmpty()) builder.append('.');
    }
    if (!name.isEmpty()) builder.append(name);
    if (builder.length() == 0) return "`?`";

    return builder.toString();
  }
}
