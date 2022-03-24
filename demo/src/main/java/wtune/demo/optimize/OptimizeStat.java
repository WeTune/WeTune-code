package wtune.demo.optimize;

import wtune.superopt.optimizer.OptimizationStep;

import java.util.List;

public record OptimizeStat(String rawSql,
                           String optSql,
                           List<OptimizationStep> rules,
                           String msg) {
  public OptimizeStat(String rawSql, String optSql, List<OptimizationStep> rules) {
    this(rawSql, optSql, rules, null);
  }

  public OptimizeStat(String rawSql, String msg) {
    this(rawSql, null, null, msg);
  }

  public boolean isOptimized() {
    return optSql != null;
  }

  public static OptimizeStat success(String rawSql, String optSql, List<OptimizationStep> rules) {
    return new OptimizeStat(rawSql, optSql, rules);
  }

  public static OptimizeStat fail(String rawSql, String msg) {
    return new OptimizeStat(rawSql, msg);
  }
}
