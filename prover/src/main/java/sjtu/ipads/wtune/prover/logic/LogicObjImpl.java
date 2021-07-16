package sjtu.ipads.wtune.prover.logic;

abstract class LogicObjImpl implements LogicObj {
  private final LogicCtx ctx;
  private final Object underlying;

  protected LogicObjImpl(LogicCtx ctx, Object underlying) {
    this.ctx = ctx;
    this.underlying = underlying;
  }

  protected LogicObjImpl() {
    this.ctx = null;
    this.underlying = null;
  }

  @Override
  public LogicCtx ctx() {
    return ctx;
  }

  @Override
  public Object underlying() {
    return underlying;
  }

  @Override
  public <T> T unwrap(Class<T> cls) {
    return (T) underlying;
  }
}
