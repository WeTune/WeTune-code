package sjtu.ipads.wtune.symsolver.smt.impl;

import sjtu.ipads.wtune.symsolver.smt.Proposition;
import sjtu.ipads.wtune.symsolver.smt.SmtCtx;
import sjtu.ipads.wtune.symsolver.smt.Value;

import static java.util.Objects.requireNonNull;

public class PropositionImpl extends ValueImpl implements Proposition {
  private PropositionImpl(SmtCtx ctx, Object underlying) {
    super(ctx, underlying);
  }

  public static Proposition build(SmtCtx ctx, Object underlying) {
    requireNonNull(ctx);
    requireNonNull(underlying);

    return new PropositionImpl(ctx, underlying);
  }

  @Override
  public Proposition not() {
    return ctx.makeNot(this);
  }

  @Override
  public Proposition implies(Proposition other) {
    return ctx.makeImplies(this, other);
  }

  @Override
  public Proposition and(Proposition other) {
    if (other == null) return this;
    return ctx.makeAnd(this, other);
  }

  @Override
  public Proposition equalsTo(Value other) {
    if (!(other instanceof Proposition)) throw new IllegalArgumentException();
    return ctx.makeEq(this, other);
  }
}
