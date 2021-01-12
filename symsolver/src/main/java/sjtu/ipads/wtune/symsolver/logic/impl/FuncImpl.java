package sjtu.ipads.wtune.symsolver.logic.impl;

import sjtu.ipads.wtune.symsolver.logic.Func;
import sjtu.ipads.wtune.symsolver.logic.Proposition;
import sjtu.ipads.wtune.symsolver.logic.LogicCtx;
import sjtu.ipads.wtune.symsolver.logic.Value;

public class FuncImpl extends ValueImpl implements Func {
  private final String name;
  private final int arity;

  private FuncImpl(LogicCtx ctx, String name, int arity, Object underlying) {
    super(ctx, underlying);
    this.arity = arity;
    this.name = name;
  }

  public static Func build(LogicCtx ctx, String name, int arity, Object underlying) {
    return new FuncImpl(ctx, name, arity, underlying);
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public int arity() {
    return arity;
  }

  @Override
  public Proposition equalsTo(Func other) {
    return ctx.makeEq(this, other);
  }

  @Override
  public Value apply(Value... args) {
    return ctx.makeApply(this, args);
  }
}
