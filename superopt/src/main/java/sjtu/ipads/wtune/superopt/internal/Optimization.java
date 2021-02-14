package sjtu.ipads.wtune.superopt.internal;

import sjtu.ipads.wtune.superopt.optimization.match.Matching;
import sjtu.ipads.wtune.superopt.optimization.Operator;
import sjtu.ipads.wtune.superopt.optimization.Substitution;
import sjtu.ipads.wtune.superopt.optimization.SubstitutionRepo;
import sjtu.ipads.wtune.superopt.optimization.match.Interpretations;
import sjtu.ipads.wtune.superopt.plan.Plan;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Optimization {
  private final Set<Operator> known = new HashSet<>();
  private SubstitutionRepo repo;

  public void optimize(Operator op) {}

  private void match(Operator op) {
    final List<Substitution> subs = match0(op);
    for (Substitution sub : subs) {
      final Matching matching = match1(op, sub);
      if (matching != null) {
        // `substituted` should be plan root instead of matching point
        final Operator substituted = applySubstitution(matching);

        // again, match the substituted plan
        if (known.add(substituted)) match(substituted);
      }
    }

    // percolate down
    for (Operator predecessor : op.predecessors()) match(predecessor);
  }

  private List<Substitution> match0(Operator op) {
    return Collections.emptyList();
  }

  private Matching match1(Operator op, Substitution sub) {
    return null;
  }

  private Operator applySubstitution(Matching matching) {
    final Operator op = instantiate(matching.substitution().g1(), matching.interpretations());
    final Operator matchingPoint = matching.matchingPoint();
    matchingPoint.successor().replacePredecessor(matchingPoint, op);
    return matching.root();
  }

  private Operator instantiate(Plan plan, Interpretations interpretations) {
    return null;
  }
}
