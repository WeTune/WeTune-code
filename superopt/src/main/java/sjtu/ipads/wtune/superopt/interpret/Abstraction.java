package sjtu.ipads.wtune.superopt.interpret;

import sjtu.ipads.wtune.superopt.interpret.impl.AbstractionImpl;

public interface Abstraction<T> {
  Abstraction<T> setInterpretation(Interpretation interpretation);

  Interpretation interpretation();

  String name();

  default T get() {
    return interpretation().interpret(this);
  }

  static <T> Abstraction<T> create(String name) {
    return AbstractionImpl.create(name);
  }
}
