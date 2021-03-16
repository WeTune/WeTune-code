package sjtu.ipads.wtune.superopt.optimizer.internal;

import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Interpretations;
import sjtu.ipads.wtune.superopt.optimizer.Match;

public record MatchImpl(PlanNode matchPoint, Interpretations assignments) implements Match {
  public static Match build(PlanNode matchPoint, Interpretations assignments) {
    return new MatchImpl(matchPoint, assignments);
  }
}
