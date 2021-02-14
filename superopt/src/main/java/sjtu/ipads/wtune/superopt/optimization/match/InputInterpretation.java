package sjtu.ipads.wtune.superopt.optimization.match;

import sjtu.ipads.wtune.sqlparser.plan.PlanNode;

public interface InputInterpretation {
  PlanNode operator();
}
