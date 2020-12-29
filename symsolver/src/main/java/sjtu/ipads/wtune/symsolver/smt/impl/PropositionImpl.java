package sjtu.ipads.wtune.symsolver.smt.impl;

import sjtu.ipads.wtune.symsolver.smt.Proposition;
import sjtu.ipads.wtune.symsolver.smt.SmtCtx;
import sjtu.ipads.wtune.symsolver.smt.Value;

import static java.util.Objects.requireNonNull;

public class PropositionImpl extends ValueImpl implements Proposition {
  private static final Proposition TAUTOLOGY = new PropositionImpl(null, null);
  private static final Proposition CONTRADICTION = new PropositionImpl(null, null);

  private PropositionImpl(SmtCtx ctx, Object underlying) {
    super(ctx, underlying);
  }

  public static Proposition build(SmtCtx ctx, Object underlying) {
    requireNonNull(ctx);
    requireNonNull(underlying);

    return new PropositionImpl(ctx, underlying);
  }

  public static Proposition tautology() {
    return TAUTOLOGY;
  }

  @Override
  public Proposition not() {
    if (this == TAUTOLOGY) return CONTRADICTION;
    if (this == CONTRADICTION) return TAUTOLOGY;
    return ctx.makeNot(this);
  }

  @Override
  public Proposition implies(Proposition other) {
    if (this == TAUTOLOGY) return other;
    if (this == CONTRADICTION) return TAUTOLOGY;
    if (other == TAUTOLOGY) return TAUTOLOGY;
    if (other == CONTRADICTION) return this.not();
    return ctx.makeImplies(this, other);
  }

  @Override
  public Proposition and(Proposition other) {
    if (this == TAUTOLOGY) return other;
    if (this == CONTRADICTION) return CONTRADICTION;
    if (other == TAUTOLOGY) return this;
    if (other == CONTRADICTION) return CONTRADICTION;
    return ctx.makeAnd(this, other);
  }

  @Override
  public Proposition equalsTo(Value other) {
    if (!(other instanceof Proposition)) throw new IllegalArgumentException();

    final Proposition otherProposition = (Proposition) other;
    if (this == TAUTOLOGY) return otherProposition;
    if (this == CONTRADICTION) return otherProposition.not();
    if (other == TAUTOLOGY) return this;
    if (other == CONTRADICTION) return this.not();

    return ctx.makeEq(this, other);
  }

  @Override
  public String toString() {
    return this == TAUTOLOGY ? "true" : super.toString();
  }
}
