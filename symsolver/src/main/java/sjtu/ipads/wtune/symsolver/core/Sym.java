package sjtu.ipads.wtune.symsolver.core;

import sjtu.ipads.wtune.symsolver.logic.Func;
import sjtu.ipads.wtune.symsolver.logic.Value;

public interface Sym extends Indexed, Scoped {
  <T> T unwrap(Class<T> cls);

  Func func();

  void setFunc(Func func);

  default String name() {
    return func().name();
  }

  default Value apply(Value... args) {
    return func().apply(args);
  }
}
