package sjtu.ipads.wtune.prover.logic;

interface Proposition extends Value {
  static Proposition wrap(LogicCtx ctx, Object underlying) {
    return PropositionImpl.build(ctx, underlying);
  }

  Proposition not();

  Proposition implies(Proposition p);

  Proposition and(Proposition p);

  Value ite(Value v0, Value v1);
}
