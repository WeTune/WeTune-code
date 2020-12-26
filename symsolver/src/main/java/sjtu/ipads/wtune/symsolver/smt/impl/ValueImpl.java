package sjtu.ipads.wtune.symsolver.smt.impl;

import sjtu.ipads.wtune.symsolver.smt.Proposition;
import sjtu.ipads.wtune.symsolver.smt.SmtCtx;
import sjtu.ipads.wtune.symsolver.smt.Value;

public class ValueImpl implements Value {
  protected final SmtCtx ctx;
  private final String name;
  private final Object underlying;

  protected ValueImpl(SmtCtx ctx, String name, Object underlying) {
    this.ctx = ctx;
    this.name = name;
    this.underlying = underlying;
  }

  public static Value build(SmtCtx ctx, String name, Object underlying) {
    return new ValueImpl(ctx, name, underlying);
  }

  @Override
  public SmtCtx ctx() {
    return ctx;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public Proposition equalsTo(Value other) {
    return ctx.makeEq(this, other);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T unwrap(Class<T> cls) {
    return ((T) underlying);
  }
}
