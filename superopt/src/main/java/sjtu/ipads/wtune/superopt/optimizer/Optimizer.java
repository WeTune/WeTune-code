package sjtu.ipads.wtune.superopt.optimizer;

import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.superopt.substitution.SubstitutionBank;

import java.util.List;
import java.util.Set;

public interface Optimizer {
  Set<PlanNode> optimize(PlanNode plan);

  void setTimeout(long timeout);

  void setTracing(boolean flag);

  List<OptimizationStep> traceOf(PlanNode plan);

  static Optimizer mk(SubstitutionBank bank) {
    return new OptimizerImpl(bank);
  }
}
