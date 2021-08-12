package sjtu.ipads.wtune.prover.logic;

import static java.util.Objects.requireNonNull;

class PropositionImpl extends ValueImpl implements Proposition {
  private PropositionImpl(LogicCtx ctx, Object underlying) {
    super(ctx, underlying);
  }

  public static Proposition build(LogicCtx ctx, Object underlying) {
    requireNonNull(ctx);
    requireNonNull(underlying);

    return new PropositionImpl(ctx, underlying);
  }

  @Override
  public Proposition not() {
    return ctx.mkNot(this);
  }

  @Override
  public Proposition implies(Proposition p) {
    return ctx.mkImplies(this, p);
  }

  @Override
  public Proposition and(Proposition p) {
    return ctx.mkConjunction(this, p);
  }

  @Override
  public Value ite(Value v0, Value v1) {
    return ctx.mkIte(this, v0, v1);
  }
}
