package sjtu.ipads.wtune.superopt.optimization.match;

import sjtu.ipads.wtune.sqlparser.plan.PlanNode;

public interface Matcher {
  MatchResult match(MatchContext ctx, PlanNode planNode);

  <T extends sjtu.ipads.wtune.superopt.plan.PlanNode> T planNode();
}
