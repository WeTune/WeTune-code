package sjtu.ipads.wtune.superopt.optimizer;

import sjtu.ipads.wtune.sql.plan.PlanContext;
import sjtu.ipads.wtune.superopt.substitution.Substitution;

public record OptimizationStep(PlanContext source,
                               PlanContext target,
                               Substitution rule,
                               int extra) {
  public int ruleId() {
    return rule == null ? -extra : rule.id();
  }
}
