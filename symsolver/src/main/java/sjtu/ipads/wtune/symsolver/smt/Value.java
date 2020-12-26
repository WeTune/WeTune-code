package sjtu.ipads.wtune.symsolver.smt;

import sjtu.ipads.wtune.symsolver.smt.impl.ValueImpl;

public interface Value {
  static Value wrap(SmtCtx ctx, String name, Object underlying) {
    return ValueImpl.build(ctx, name, underlying);
  }

  SmtCtx ctx();

  String name();

  Proposition equalsTo(Value other);

  <T> T unwrap(Class<T> cls);
}
