package sjtu.ipads.wtune.superopt.interpret.impl;

import sjtu.ipads.wtune.superopt.interpret.Abstraction;

public class AbstractionImpl<T> implements Abstraction<T> {
  private final Object context;
  private final String name;

  private AbstractionImpl(Object context, String name) {
    this.context = context;
    this.name = name;
  }

  public static <T> AbstractionImpl<T> create(Object context, String name) {
    return new AbstractionImpl<>(context, name == null ? "anony" : name);
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public Object context() {
    return context;
  }

  @Override
  public String toString() {
    return "Abstract<'" + name + "'>";
  }
}
