package sjtu.ipads.wtune.symsolver.search;

import sjtu.ipads.wtune.symsolver.search.impl.CombinedProver;
import sjtu.ipads.wtune.symsolver.search.impl.IncrementalProver;
import sjtu.ipads.wtune.symsolver.search.impl.OneShotProver;
import sjtu.ipads.wtune.symsolver.smt.Proposition;
import sjtu.ipads.wtune.symsolver.smt.SmtCtx;

public interface Prover extends Reactor {
  static Prover oneShot(SmtCtx ctx, Proposition problem) {
    return OneShotProver.build(ctx, problem);
  }

  static Prover incremental(SmtCtx ctx, Proposition problem) {
    return IncrementalProver.build(ctx, problem);
  }

  static Prover combine(Prover... provers) {
    return CombinedProver.build(provers);
  }

  void prepare(Decision[] choices);

  boolean prove();
}
