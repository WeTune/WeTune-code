package sjtu.ipads.wtune.symsolver.search.impl;

import sjtu.ipads.wtune.symsolver.search.Decision;
import sjtu.ipads.wtune.symsolver.search.Prover;
import sjtu.ipads.wtune.symsolver.smt.Proposition;
import sjtu.ipads.wtune.symsolver.smt.SmtCtx;
import sjtu.ipads.wtune.symsolver.smt.SmtSolver;

import static java.util.Collections.singletonMap;

public class OneShotProver extends BaseProver {
  private final SmtSolver smtSolver;

  private OneShotProver(SmtCtx ctx, Proposition problem) {
    super(ctx, problem);
    this.smtSolver = ctx.makeSolver(singletonMap("tactic", "uflra"));
  }

  public static Prover build(SmtCtx ctx, Proposition problem) {
    return new OneShotProver(ctx, problem);
  }

  @Override
  public boolean prove() {
    smtSolver.add(problem);
    smtSolver.add(baseAssertions);
    for (Decision decision : decisions) assertions.get(decision).forEach(smtSolver::add);

    final SmtSolver.Result res = smtSolver.check();
    smtSolver.reset();

    return res == SmtSolver.Result.UNSAT;
  }
}
