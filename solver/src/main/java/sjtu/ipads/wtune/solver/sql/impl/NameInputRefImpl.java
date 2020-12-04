package sjtu.ipads.wtune.solver.sql.impl;

import sjtu.ipads.wtune.solver.sql.expr.NameInputRef;

public class NameInputRefImpl implements NameInputRef {
  private final String name;

  private NameInputRefImpl(String name) {
    this.name = name;
  }

  public static NameInputRef create(String name) {
    return new NameInputRefImpl(name);
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
