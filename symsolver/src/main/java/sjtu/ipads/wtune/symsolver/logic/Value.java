package sjtu.ipads.wtune.symsolver.logic;

import sjtu.ipads.wtune.symsolver.logic.impl.ValueImpl;

public interface Value {
  static Value wrap(LogicCtx ctx, Object underlying) {
    return ValueImpl.build(ctx, underlying);
  }

  LogicCtx ctx();

  Proposition equalsTo(Value other);

  <T> T unwrap(Class<T> cls);
}
