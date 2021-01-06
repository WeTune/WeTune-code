package sjtu.ipads.wtune.symsolver.logic;

import sjtu.ipads.wtune.symsolver.logic.impl.ValueImpl;

public interface Value {
  static Value wrap(SmtCtx ctx, Object underlying) {
    return ValueImpl.build(ctx, underlying);
  }

  SmtCtx ctx();

  Proposition equalsTo(Value other);

  <T> T unwrap(Class<T> cls);
}
