package sjtu.ipads.wtune.symsolver.search.impl;

import sjtu.ipads.wtune.symsolver.core.Result;
import sjtu.ipads.wtune.symsolver.search.Decision;
import sjtu.ipads.wtune.symsolver.search.Prover;
import sjtu.ipads.wtune.symsolver.smt.Proposition;
import sjtu.ipads.wtune.symsolver.smt.SmtCtx;

public class OneShotProver extends BaseProver {
  private OneShotProver(SmtCtx ctx, Proposition problem) {
    super(ctx, problem);
  }

  public static Prover build(SmtCtx ctx, Proposition problem) {
    return new OneShotProver(ctx, problem);
  }

  @Override
  public Result prove() {
    smtSolver.add(problem);
    smtSolver.add(baseAssertions);
    for (Decision decision : decisions) assertions.get(decision).forEach(smtSolver::add);

    final Result res = smtSolver.check();
    smtSolver.reset();

    return res;
  }
}
