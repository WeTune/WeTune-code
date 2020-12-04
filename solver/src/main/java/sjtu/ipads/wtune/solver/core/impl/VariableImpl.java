package sjtu.ipads.wtune.solver.core.impl;

import sjtu.ipads.wtune.solver.core.Variable;

public class VariableImpl implements Variable {
  private final Object expr;
  private final String name;

  private VariableImpl(Object expr, String name) {
    this.expr = expr;
    this.name = name;
  }

  public static VariableImpl create(Object expr, String name) {
    return new VariableImpl(expr, name);
  }

  @Override
  public <T> T unwrap(Class<T> clazz) {
    return (T) expr;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public String toString() {
    return name;
  }
}
