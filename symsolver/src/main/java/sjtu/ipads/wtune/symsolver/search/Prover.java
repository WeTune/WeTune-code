package sjtu.ipads.wtune.symsolver.search;

import sjtu.ipads.wtune.symsolver.core.Query;
import sjtu.ipads.wtune.symsolver.core.Result;
import sjtu.ipads.wtune.symsolver.search.impl.CombinedProver;
import sjtu.ipads.wtune.symsolver.search.impl.IncrementalProver;
import sjtu.ipads.wtune.symsolver.search.impl.OneShotProver;
import sjtu.ipads.wtune.symsolver.logic.LogicCtx;

public interface Prover extends Reactor {
  static Prover oneShot(LogicCtx ctx, Query q0, Query q1) {
    return OneShotProver.build(ctx, q0, q1);
  }

  static Prover incremental(LogicCtx ctx, Query q0, Query q1) {
    return IncrementalProver.build(ctx, q0, q1);
  }

  static Prover combine(Prover... provers) {
    return CombinedProver.build(provers);
  }

  void prepare(Decision[] choices);

  Result prove();
}
