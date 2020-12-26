package sjtu.ipads.wtune.symsolver.smt.impl;

import sjtu.ipads.wtune.symsolver.smt.Func;
import sjtu.ipads.wtune.symsolver.smt.Proposition;
import sjtu.ipads.wtune.symsolver.smt.SmtCtx;
import sjtu.ipads.wtune.symsolver.smt.Value;

public class FuncImpl extends ValueImpl implements Func {
  private final int arity;

  private FuncImpl(SmtCtx ctx, String name, int arity, Object underlying) {
    super(ctx, name, underlying);
    this.arity = arity;
  }

  public static Func build(SmtCtx ctx, String name, int arity, Object underlying) {
    return new FuncImpl(ctx, name, arity, underlying);
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
