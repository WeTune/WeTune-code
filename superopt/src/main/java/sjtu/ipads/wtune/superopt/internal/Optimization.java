package sjtu.ipads.wtune.superopt.internal;

import sjtu.ipads.wtune.superopt.optimization.match.Matching;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.superopt.optimization.Substitution;
import sjtu.ipads.wtune.superopt.optimization.SubstitutionRepo;
import sjtu.ipads.wtune.superopt.optimization.match.Interpretations;
import sjtu.ipads.wtune.superopt.plan.Plan;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Optimization {
  private final Set<PlanNode> known = new HashSet<>();
  private SubstitutionRepo repo;

  public void optimize(PlanNode op) {}

  private void match(PlanNode op) {
    final List<Substitution> subs = match0(op);
    for (Substitution sub : subs) {
      final Matching matching = match1(op, sub);
      if (matching != null) {
        // `substituted` should be plan root instead of matching point
        final PlanNode substituted = applySubstitution(matching);

        // again, match the substituted plan
        if (known.add(substituted)) match(substituted);
      }
    }

    // percolate down
    for (PlanNode predecessor : op.predecessors()) match(predecessor);
  }

  private List<Substitution> match0(PlanNode op) {
    return Collections.emptyList();
  }

  private Matching match1(PlanNode op, Substitution sub) {
    return null;
  }

  private PlanNode applySubstitution(Matching matching) {
    final PlanNode op = instantiate(matching.substitution().g1(), matching.interpretations());
    final PlanNode matchingPoint = matching.matchingPoint();
    matchingPoint.successor().replacePredecessor(matchingPoint, op);
    return matching.root();
  }

  private PlanNode instantiate(Plan plan, Interpretations interpretations) {
    return null;
  }
}
