package sjtu.ipads.wtune.symsolver.smt.impl;

import sjtu.ipads.wtune.symsolver.smt.Proposition;
import sjtu.ipads.wtune.symsolver.smt.SmtCtx;

public class PropositionImpl extends ValueImpl implements Proposition {
  private static final Proposition TAUTOLOGY = build(null, null, null);

  private PropositionImpl(SmtCtx ctx, String name, Object underlying) {
    super(ctx, name, underlying);
  }

  public static Proposition build(SmtCtx ctx, String name, Object underlying) {
    return new PropositionImpl(ctx, name, underlying);
  }

  public static Proposition tautology() {
    return TAUTOLOGY;
  }

  @Override
  public Proposition implies(Proposition other) {
    if (this == TAUTOLOGY) return other;
    if (other == TAUTOLOGY) return TAUTOLOGY;
    return ctx.makeImplies(this, other);
  }

  @Override
  public Proposition and(Proposition other) {
    if (this == TAUTOLOGY) return other;
    if (other == TAUTOLOGY) return this;
    return ctx.makeAnd(this, other);
  }
}
