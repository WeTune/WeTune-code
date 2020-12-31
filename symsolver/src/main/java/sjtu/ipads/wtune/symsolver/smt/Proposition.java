package sjtu.ipads.wtune.symsolver.smt;

import sjtu.ipads.wtune.symsolver.smt.impl.PropositionImpl;

public interface Proposition extends Value {
  static Proposition wrap(SmtCtx ctx, Object underlying) {
    return PropositionImpl.build(ctx, underlying);
  }

  Proposition not();

  Proposition implies(Proposition other);

  Proposition and(Proposition other);
}
