package sjtu.ipads.wtune.symsolver.search.impl;

import sjtu.ipads.wtune.symsolver.core.Query;
import sjtu.ipads.wtune.symsolver.core.Result;
import sjtu.ipads.wtune.symsolver.search.Decision;
import sjtu.ipads.wtune.symsolver.search.Prover;
import sjtu.ipads.wtune.symsolver.smt.SmtCtx;

public class OneShotProver extends BaseProver {
  private OneShotProver(SmtCtx ctx, Query q0, Query q1) {
    super(ctx, q0, q1);
  }

  public static Prover build(SmtCtx ctx, Query q0, Query q1) {
    return new OneShotProver(ctx, q0, q1);
  }

  @Override
  public Result prove() {
    smtSolver.add(targetProperties);
    for (Decision decision : decisions) assertions.get(decision).forEach(smtSolver::add);

    final Result res = smtSolver.check();
    smtSolver.reset();

    return res;
  }
}
