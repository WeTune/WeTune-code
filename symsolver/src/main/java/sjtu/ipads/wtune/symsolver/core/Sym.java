package sjtu.ipads.wtune.symsolver.core;

import sjtu.ipads.wtune.symsolver.smt.Func;
import sjtu.ipads.wtune.symsolver.smt.Value;
import sjtu.ipads.wtune.symsolver.utils.Indexed;

public interface Sym extends Indexed {
  <T> T unwrap(Class<T> cls);

  Object scope();

  Func func();

  void setFunc(Func func);

  default String name() {
    return func().name();
  }

  default Value apply(Value... args) {
    return func().apply(args);
  }
}
