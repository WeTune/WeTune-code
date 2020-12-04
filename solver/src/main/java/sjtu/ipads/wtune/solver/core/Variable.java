package sjtu.ipads.wtune.solver.core;

import sjtu.ipads.wtune.solver.core.impl.VariableImpl;

public interface Variable {
  static Variable wrap(Object obj) {
    return VariableImpl.create(obj, null);
  }

  static Variable wrap(Object obj, String name) {
    return VariableImpl.create(obj, name);
  }

  String name();

  <T> T unwrap(Class<T> clazz);
}
