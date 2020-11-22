package sjtu.ipads.wtune.superopt.interpret;

import sjtu.ipads.wtune.superopt.interpret.impl.AbstractionImpl;

public interface Abstraction<T> {
  Abstraction<T> setInterpretation(Interpretation interpretation);

  Interpretation interpretation();

  String name();

  default boolean interpreted(Interpretation interpretation) {
    return interpretation.interpret(this) != null;
  }

  default T get() {
    return interpretation() == null ? null : interpretation().interpret(this);
  }

  static <T> Abstraction<T> create(String name) {
    return AbstractionImpl.create(name);
  }
}
