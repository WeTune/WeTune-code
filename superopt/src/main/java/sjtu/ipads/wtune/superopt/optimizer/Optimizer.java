package sjtu.ipads.wtune.superopt.optimizer;

import sjtu.ipads.wtune.sql.ast.SqlNode;
import sjtu.ipads.wtune.sql.plan.PlanContext;
import sjtu.ipads.wtune.sql.plan.PlanSupport;
import sjtu.ipads.wtune.sql.schema.Schema;
import sjtu.ipads.wtune.superopt.substitution.SubstitutionBank;

import java.util.List;
import java.util.Set;

import static java.util.Collections.emptySet;

public interface Optimizer {
  Set<PlanContext> optimize(PlanContext plan);

  void setTimeout(long timeout);

  void setTracing(boolean flag);

  void setVerbose(boolean flag);

  List<OptimizationStep> traceOf(PlanContext plan);

  static Optimizer mk(SubstitutionBank bank) {
    return new BottomUpOptimizer(bank);
  }

  default Set<PlanContext> optimize(SqlNode sql) {
    return optimize(sql, sql.context().schema());
  }

  default Set<PlanContext> optimize(SqlNode sql, Schema schema) {
    final PlanContext plan = PlanSupport.assemblePlan(sql, schema);
    if (plan != null) return optimize(plan);
    else return emptySet();
  }
}
