package sjtu.ipads.wtune.superopt.optimizer;

import sjtu.ipads.wtune.sqlparser.plan.PlanContext;
import sjtu.ipads.wtune.superopt.substitution.Substitution;

public record OptimizationStep(PlanContext source,
                               PlanContext target,
                               Substitution rule) {
  public int ruleId() {
    return rule == null ? -1 : rule.id();
  }
}
