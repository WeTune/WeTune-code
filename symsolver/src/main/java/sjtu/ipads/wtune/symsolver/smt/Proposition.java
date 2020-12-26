package sjtu.ipads.wtune.symsolver.smt;

import sjtu.ipads.wtune.symsolver.smt.impl.PropositionImpl;

public interface Proposition extends Value {
  static Proposition wrap(SmtCtx ctx, String name, Object underlying) {
    return PropositionImpl.build(ctx, name, underlying);
  }

  static Proposition tautology() {
    return PropositionImpl.tautology();
  }

  Proposition implies(Proposition other);

  Proposition and(Proposition other);
}
