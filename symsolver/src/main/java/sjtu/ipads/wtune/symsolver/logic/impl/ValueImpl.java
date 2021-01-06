package sjtu.ipads.wtune.symsolver.logic.impl;

import sjtu.ipads.wtune.symsolver.logic.Proposition;
import sjtu.ipads.wtune.symsolver.logic.SmtCtx;
import sjtu.ipads.wtune.symsolver.logic.Value;

import java.util.Objects;

public class ValueImpl implements Value {
  protected final SmtCtx ctx;
  private final Object underlying;

  protected ValueImpl(SmtCtx ctx, Object underlying) {
    this.ctx = ctx;
    this.underlying = underlying;
  }

  public static Value build(SmtCtx ctx, Object underlying) {
    return new ValueImpl(ctx, underlying);
  }

  @Override
  public SmtCtx ctx() {
    return ctx;
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

  @Override
  public String toString() {
    return Objects.toString(underlying);
  }
}
