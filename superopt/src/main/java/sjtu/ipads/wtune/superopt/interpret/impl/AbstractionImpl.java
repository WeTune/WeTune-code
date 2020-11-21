package sjtu.ipads.wtune.superopt.interpret.impl;

import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;

public class AbstractionImpl<T> implements Abstraction<T> {
  private Interpretation interpretation;
  private final String name;

  private AbstractionImpl(String name) {
    this.name = name;
  }

  public static <T> AbstractionImpl<T> create(String name) {
    return new AbstractionImpl<>(name == null ? "anony" : name);
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public Abstraction<T> setInterpretation(Interpretation interpretation) {
    this.interpretation = interpretation;
    return this;
  }

  @Override
  public Interpretation interpretation() {
    return interpretation;
  }

  @Override
  public String toString() {
    return "Abstract<'" + name + "', " + get() + ">";
  }
}
