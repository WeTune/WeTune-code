package wtune.superopt.profiler;

import wtune.sql.plan.PlanContext;

import java.util.Properties;

public interface Profiler {
  void setBaseline(PlanContext baseline);

  PlanContext getBaseline();

  void profile(PlanContext plan);

  PlanContext getPlan(int index);

  double getCost(int index);

  double getBaselineCost();

  int minCostIndex();

  int minCostIndexOfCandidates();

  static Profiler mk(Properties dbProps) {
    return new ProfilerImpl(dbProps);
  }
}
