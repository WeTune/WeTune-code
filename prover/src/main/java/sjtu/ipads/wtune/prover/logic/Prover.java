package sjtu.ipads.wtune.prover.logic;

import static java.util.Collections.singletonList;

import sjtu.ipads.wtune.prover.logic.LogicSolver.Result;
import sjtu.ipads.wtune.prover.normalform.Disjunction;

public class Prover {
  private final LogicCtx ctx;
  private final LogicSolver solver;

  private Prover(LogicCtx ctx) {
    this.ctx = ctx;
    this.solver = ctx.mkSolver();
  }

  public static Prover mk(LogicCtx ctx) {
    return new Prover(ctx);
  }

  public boolean prove(Disjunction expr0, Disjunction expr1) {
    final LogicTranslator translator = LogicTranslator.mk(ctx);
    final Value v0 = translator.translate(expr0);
    final Value v1 = translator.translate(expr1);
    final Proposition proposition = v0.eq(v1).not();
    solver.reset();
    solver.add(translator.assertions());
    solver.add(singletonList(proposition));
    return solver.solve() == Result.UNSAT;
  }
}
