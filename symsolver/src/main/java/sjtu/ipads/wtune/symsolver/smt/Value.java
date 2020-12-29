package sjtu.ipads.wtune.symsolver.smt;

import sjtu.ipads.wtune.symsolver.smt.impl.ValueImpl;

public interface Value {
  static Value wrap(SmtCtx ctx, Object underlying) {
    return ValueImpl.build(ctx, underlying);
  }

  SmtCtx ctx();

  Proposition equalsTo(Value other);

  <T> T unwrap(Class<T> cls);
}
