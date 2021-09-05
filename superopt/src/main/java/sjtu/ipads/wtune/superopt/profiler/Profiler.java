package sjtu.ipads.wtune.superopt.profiler;

import sjtu.ipads.wtune.sqlparser.plan.PlanNode;

import java.util.Properties;

public interface Profiler {
  void setBaseline(PlanNode baseline);

  void profile(PlanNode plan);

  PlanNode getPlan(int index);

  double getCost(int index);

  int minCostIndex();

  static Profiler mk(Properties dbProps) {
    return new ProfilerImpl(dbProps);
  }
}
