package sjtu.ipads.wtune.prover.logic;

class FuncImpl extends ValueImpl implements Func {
  private final String name;
  private final DataType[] paramTypes;

  FuncImpl(LogicCtx ctx, Object underlying, String name, DataType[] paramTypes) {
    super(ctx, underlying);
    this.name = name;
    this.paramTypes = paramTypes;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public DataType[] paramTypes() {
    return paramTypes;
  }

  @Override
  public Value apply(Value... args) {
    return ctx.mkApply(this, args);
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Func)) return false;
    final Func func = (Func) o;
    return name().equals(func.name());
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }
}
