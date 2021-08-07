package sjtu.ipads.wtune.prover.logic;

import java.util.Objects;

class ValueImpl implements Value {
  protected final LogicCtx ctx;
  private final Object underlying;

  protected ValueImpl(LogicCtx ctx, Object underlying) {
    this.ctx = ctx;
    this.underlying = underlying;
  }

  public static Value build(LogicCtx ctx, Object underlying) {
    return new ValueImpl(ctx, underlying);
  }

  @Override
  public LogicCtx ctx() {
    return ctx;
  }

  @Override
  public Proposition gt(int i) {
    return ctx.mkGt(this, ctx.mkConst(i));
  }

  @Override
  public Proposition ge(int i) {
    return ctx.mkGe(this, ctx.mkConst(i));
  }

  @Override
  public Value mul(Value other) {
    return ctx.mkProduct(this, other);
  }

  @Override
  public Proposition eq(Value other) {
    return ctx.mkEq(this, other);
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ValueImpl value = (ValueImpl) o;
    return underlying.equals(value.underlying);
  }

  @Override
  public int hashCode() {
    return Objects.hash(underlying);
  }
}
