package sjtu.ipads.wtune.superopt.internal;

import sjtu.ipads.wtune.common.multiversion.Snapshot;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan.ToASTTranslator;
import sjtu.ipads.wtune.superopt.fragment.Operator;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Interpretations;
import sjtu.ipads.wtune.superopt.optimization.*;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.common.utils.FuncUtils.stream;

public class Optimizer {
  private final List<PlanNode> optimized = new ArrayList<>();
  private final Set<String> known = new HashSet<>();
  private final SubstitutionRepo repo;

  public Optimizer(SubstitutionRepo repo) {
    this.repo = repo;
  }

  public List<PlanNode> optimize(PlanNode op) {
    optimized.clear();
    known.clear();
    matchAndSubstitute(op);
    return optimized;
  }

  private void matchAndSubstitute(PlanNode op) {
    for (Substitution sub : match0(op)) {
      final Interpretations interpretations = Interpretations.build(sub.constraints());
      for (Matching matching : match1(op, sub.g0().head(), interpretations)) {
        // impl note: `substituted` should be plan root instead of matching point
        final PlanNode substituted = matching.substitute(sub.g1(), interpretations);
        // match the substituted plan from beginning
        if (known.add(ToASTTranslator.translate(substituted).toString())) {
          optimized.add(substituted);
          matchAndSubstitute(substituted);
        }
      }
    }
    // percolate down
    for (PlanNode predecessor : op.predecessors()) matchAndSubstitute(predecessor);
  }

  private Iterable<Substitution> match0(PlanNode op) {
    return stream(Fingerprint.make(op))
        .map(repo::findByFingerprint)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  private static List<Matching> match1(PlanNode node, Operator op, Interpretations inter) {
    final List<Matching> ret = new ArrayList<>();

    for (PlanNode n : Hint.apply(node, op, inter)) {
      // setup snapshot to isolate each possible plan given by hint
      final Snapshot snapshot = inter.snapshot();
      inter.derive();

      if (!op.match(n, inter)) {
        inter.setSnapshot(snapshot);
        continue;
      }

      final PlanNode[] nodePreds = n.predecessors();
      final Operator[] opPreds = op.predecessors();

      // Subtlety here is that
      // 1. each child may have multiple matching
      // 2. matching of the next child is affected by how the previous child is matched.
      // In other words, the final matching is accumulated by the matching of each child.
      //
      // Thus, a list `lastResults` is maintained, represents the accumulated matching so far.
      // The matching of each child will be based on each of `lastResults`.

      List<Matching> lastResults = singletonList(Matching.build(n, inter.snapshot()));
      for (int i = 0, bound = opPreds.length; i < bound; i++) {
        // failed to match last child, break
        if (lastResults.isEmpty()) break;

        final PlanNode nodePred = nodePreds[i];
        final Operator opPred = opPreds[i];

        final List<Matching> currentResults = new ArrayList<>();
        // based on each matching of last child, try to match current child
        for (Matching lastResult : lastResults) {
          // 1. restore the interpretation of last result -- the meaning of "based on"
          inter.setSnapshot(lastResult.interpretation());
          // 2. current child reports multiple matching
          final List<Matching> results = match1(nodePred, opPred, inter);
          // 3. accumulate the results to `lastResult`
          final int idx = i;
          currentResults.addAll(listMap(it -> accumulateMatching(lastResult, idx, it), results));
        }

        lastResults = currentResults;
      }

      ret.addAll(lastResults);

      inter.setSnapshot(snapshot);
    }

    return ret;
  }

  private static Matching accumulateMatching(Matching base, int predecessorIdx, Matching acc) {
    final PlanNode copy = base.matchPoint().copy();
    copy.setPredecessor(predecessorIdx, acc.matchPoint());
    return Matching.build(copy, acc.interpretation());
  }
}
