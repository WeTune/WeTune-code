package sjtu.ipads.wtune.superopt.optimization.internal;

import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Interpretations;
import sjtu.ipads.wtune.superopt.optimization.Match;

public record MatchingImpl(PlanNode matchPoint, Interpretations assignments) implements Match {
  public static Match build(PlanNode matchPoint, Interpretations assignments) {
    return new MatchingImpl(matchPoint, assignments);
  }
}
