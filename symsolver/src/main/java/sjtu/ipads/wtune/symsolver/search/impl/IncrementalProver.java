package sjtu.ipads.wtune.symsolver.search.impl;

import com.google.common.collect.Sets;
import sjtu.ipads.wtune.symsolver.DecidableConstraint;
import sjtu.ipads.wtune.symsolver.core.Query;
import sjtu.ipads.wtune.symsolver.core.Result;
import sjtu.ipads.wtune.symsolver.logic.LogicCtx;
import sjtu.ipads.wtune.symsolver.logic.Proposition;
import sjtu.ipads.wtune.symsolver.search.Decision;
import sjtu.ipads.wtune.symsolver.search.Prover;

import java.util.*;

public class IncrementalProver extends BaseProver {
  private final Map<Decision, Proposition> trackers;

  public IncrementalProver(LogicCtx ctx, Query q0, Query q1) {
    super(ctx, q0, q1);
    this.trackers = new HashMap<>();
  }

  public static Prover build(LogicCtx ctx, Query q0, Query q1) {
    return new IncrementalProver(ctx, q0, q1);
  }

  private static Proposition[] toAssumptions(
      Decision[] decisions, Map<Decision, Proposition> trackers) {
    //    final Proposition[] enabledTrackers = new Proposition[decisions.length];
    //    for (int i = 0, bound = decisions.length; i < bound; i++)
    //      enabledTrackers[i] = trackers.get(decisions[i]);
    //    return enabledTrackers;
    final Proposition[] ps = new Proposition[trackers.size()];
    final Set<Decision> set = new HashSet<>(Arrays.asList(decisions));
    int i = 0;
    for (Decision decision : set) ps[i++] = trackers.get(decision);
    for (Decision decision : Sets.difference(trackers.keySet(), set))
      ps[i++] = trackers.get(decision).not();
    return ps;
  }

  @Override
  protected void addAssertion(DecidableConstraint constraint, Proposition assertion) {
    super.addAssertion(constraint, assertion);
    trackers.computeIfAbsent(constraint, k -> ctx.makeTracker(k.toString()));
  }

  @Override
  public void prepare(Decision[] choices) {
    super.prepare(choices);
    smtSolver.add(targetProperties);
    for (Decision choice : choices) {
      final Proposition tracker = trackers.get(choice);
      for (Proposition assertion : assertions.get(choice))
        smtSolver.add(tracker.implies(assertion));
    }
  }

  @Override
  public Result prove() {
    final Proposition[] assumptions = toAssumptions(decisions, trackers);
    return smtSolver.checkAssumption(assumptions);
  }
}
