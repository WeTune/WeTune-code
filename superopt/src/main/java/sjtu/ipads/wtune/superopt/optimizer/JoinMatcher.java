package sjtu.ipads.wtune.superopt.optimizer;

import sjtu.ipads.wtune.sqlparser.plan1.PlanContext;
import sjtu.ipads.wtune.superopt.fragment.Join;

import java.util.LinkedList;
import java.util.List;

import static java.util.Collections.emptyList;

class JoinMatcher {
  private final Join op;
  private final LinearJoinTree joinTree;

  JoinMatcher(Join op, PlanContext ctx, int joinTreeRoot) {
    this.op = op;
    this.joinTree = LinearJoinTree.mk(ctx, joinTreeRoot);
  }

  List<Match> matchBasedOn(Match baseMatch) {
    if (joinTree == null) return emptyList();

    final List<Match> matches = new LinkedList<>();
    for (int i = joinTree.numJoiners() - 1; i >= -1; --i) {
      final Match match = tryMatchAt(i, baseMatch);
      if (match != null) matches.add(match);
    }

    return matches;
  }

  private Match tryMatchAt(int rootJoineeIdx, Match baseMatch) {
    if (!joinTree.isEligibleRoot(rootJoineeIdx)) return null;

    final PlanContext newPlan = joinTree.mkRootedBy(rootJoineeIdx);
    final Match derived = baseMatch.derive().setSourcePlan(newPlan);

    if (derived.matchOne(op, joinTree.joinerOf(rootJoineeIdx))) return derived;
    else return null;
  }
}
