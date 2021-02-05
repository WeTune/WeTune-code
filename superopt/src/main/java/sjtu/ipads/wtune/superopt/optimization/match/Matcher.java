package sjtu.ipads.wtune.superopt.optimization.match;

import sjtu.ipads.wtune.superopt.plan.PlanNode;
import sjtu.ipads.wtune.superopt.optimization.Operator;

public interface Matcher {
  MatchResult match(MatchContext ctx, Operator operator);

  <T extends PlanNode> T planNode();
}
