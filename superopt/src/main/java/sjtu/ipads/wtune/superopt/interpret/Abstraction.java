package sjtu.ipads.wtune.superopt.interpret;

import sjtu.ipads.wtune.superopt.interpret.impl.AbstractionImpl;

public interface Abstraction<T> {
  String name();

  Object context();

  default boolean interpreted(Interpretation interpretation) {
    return interpretation.interpret(this) != null;
  }

  static <T> Abstraction<T> create(Object context, String name) {
    return AbstractionImpl.create(context, name);
  }
}
