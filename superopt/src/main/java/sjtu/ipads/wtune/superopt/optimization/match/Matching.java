package sjtu.ipads.wtune.superopt.optimization.match;

import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.superopt.optimization.Substitution;

public interface Matching {
  PlanNode root();

  PlanNode matchingPoint();

  Substitution substitution();

  Interpretations interpretations();
}
