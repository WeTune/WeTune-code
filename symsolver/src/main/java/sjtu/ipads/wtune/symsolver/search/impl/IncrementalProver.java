package sjtu.ipads.wtune.symsolver.search.impl;

import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.search.Decision;
import sjtu.ipads.wtune.symsolver.search.Prover;
import sjtu.ipads.wtune.symsolver.smt.Proposition;
import sjtu.ipads.wtune.symsolver.smt.SmtCtx;
import sjtu.ipads.wtune.symsolver.smt.SmtSolver;

import java.util.HashMap;
import java.util.Map;

public class IncrementalProver extends BaseProver {
  private final SmtSolver smtSolver;
  private final Map<Decision, Proposition> trackers;

  public IncrementalProver(SmtCtx ctx, Proposition problem) {
    super(ctx, problem);
    this.smtSolver = ctx.makeSolver();
    this.trackers = new HashMap<>();
  }

  public static Prover build(SmtCtx ctx, Proposition problem) {
    return new IncrementalProver(ctx, problem);
  }

  private static Proposition[] toAssumptions(
      Decision[] decisions, Map<Decision, Proposition> trackers) {
    final Proposition[] enabledTrackers = new Proposition[decisions.length];
    for (int i = 0, bound = decisions.length; i < bound; i++)
      enabledTrackers[i] = trackers.get(decisions[i]);
    return enabledTrackers;
  }

  @Override
  protected void addAssertion(Constraint constraint, Proposition assertion) {
    super.addAssertion(constraint, assertion);
    trackers.computeIfAbsent(constraint, k -> ctx.makeBool(k.toString()));
  }

  @Override
  public void prepare(Decision[] choices) {
    super.prepare(choices);
    smtSolver.add(problem);
    smtSolver.add(baseAssertions);
    for (Decision choice : choices) {
      final Proposition tracker = trackers.get(choice);
      for (Proposition assertion : assertions.get(choice)) smtSolver.add(tracker.implies(assertion));
    }
  }

  @Override
  public boolean prove() {
    final Proposition[] assumptions = toAssumptions(decisions, trackers);
    return smtSolver.checkAssumption(assumptions) == SmtSolver.Result.UNSAT;
  }
}
