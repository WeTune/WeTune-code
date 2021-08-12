package sjtu.ipads.wtune.prover.logic;

interface Value {
  static Value wrap(LogicCtx ctx, Object underlying) {
    return ValueImpl.build(ctx, underlying);
  }

  Proposition gt(int i);

  Proposition ge(int i);

  Value mul(Value other);

  Proposition eq(Value other);

  LogicCtx ctx();

  <T> T unwrap(Class<T> cls);
}
