package wtune.demo.optimize;

import wtune.common.datasource.DbSupport;
import wtune.sql.SqlSupport;
import wtune.sql.ast.SqlNode;
import wtune.sql.plan.PlanContext;
import wtune.sql.plan.PlanSupport;
import wtune.sql.schema.Schema;
import wtune.superopt.optimizer.OptimizationStep;
import wtune.superopt.optimizer.Optimizer;
import wtune.superopt.profiler.Profiler;
import wtune.superopt.substitution.SubstitutionBank;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static wtune.sql.plan.PlanSupport.assemblePlan;
import static wtune.sql.plan.PlanSupport.translateAsAst;
import static wtune.sql.support.action.NormalizationSupport.normalizeAst;
import static wtune.superopt.optimizer.OptimizerSupport.*;

public class OptimizeSQLSupport {
  private static final String SQL_PARSE_ERR_MSG = "Error in parsing SQL query.";
  private static final String CANNOT_OPTIMIZE_MSG = "This SQL cannot be further optimized.";
  private static final String OPT_SQL_PARSE_ERR_MSG = "Error in parsing optimized SQL query.";
  private static final Integer TIME_OUT_MS = 10000;

  static {
    addOptimizerTweaks(TWEAK_ENABLE_EXTENSIONS);
    addOptimizerTweaks(TWEAK_SORT_FILTERS_BEFORE_OUTPUT);
  }

  /** Rewrite a SQL and return all rewritten SQLs */
  public static OptimizeStat optimizeSQL(
      String rawSql, Schema schema, SubstitutionBank rules) {
    final PlanContext plan = parsePlan(rawSql, schema.dbType(), schema);
    if (plan == null) return OptimizeStat.fail(rawSql, SQL_PARSE_ERR_MSG);

    // Rewrite this SQL, output multiple SQLs
    final Optimizer optimizer = Optimizer.mk(rules);
    optimizer.setTimeout(TIME_OUT_MS);
    optimizer.setTracing(true);

    final Set<PlanContext> optimized = optimizer.optimize(plan);
    if (optimized.isEmpty()) return OptimizeStat.fail(rawSql, CANNOT_OPTIMIZE_MSG);

    final List<String> optSqls = new ArrayList<>();
    final List<List<OptimizationStep>> ruleSteps = new ArrayList<>();
    for (PlanContext optPlan : optimized) {
      final SqlNode optAst = translateAsAst(optPlan, optPlan.root(), false);
      if (optAst == null) continue;
      optSqls.add(optAst.toString(false));
      ruleSteps.add(optimizer.traceOf(optPlan));
    }
    return OptimizeStat.success(rawSql, optSqls, ruleSteps);
  }

  /** Rewrite a SQL and pick the one with minimum cost */
  public static OptimizeStat optimizeSQLToMinCost(
      String rawSql, String appName, Schema schema, SubstitutionBank rules) {
    final PlanContext plan = parsePlan(rawSql, schema.dbType(), schema);
    if (plan == null) return OptimizeStat.fail(rawSql, SQL_PARSE_ERR_MSG);

    // Rewrite this SQL, output multiple SQLs
    final Optimizer optimizer = Optimizer.mk(rules);
    optimizer.setTimeout(TIME_OUT_MS);
    optimizer.setTracing(true);

    final Set<PlanContext> optimized = optimizer.optimize(plan);
    if (optimized.isEmpty()) return OptimizeStat.fail(rawSql, CANNOT_OPTIMIZE_MSG);

    // Pick the one with minimum cost
    final Profiler profiler = Profiler.mk(mkDbProps(schema.dbType(), appName));
    profiler.setBaseline(plan);
    for (PlanContext candidate : optimized) profiler.profile(candidate);
    final int idx = profiler.minCostIndex();
    if (idx < 0) return OptimizeStat.fail(rawSql, CANNOT_OPTIMIZE_MSG);

    final PlanContext optPlan = profiler.getPlan(idx);
    final SqlNode optAst = translateAsAst(optPlan, optPlan.root(), false);
    if (optAst == null) return OptimizeStat.fail(rawSql, OPT_SQL_PARSE_ERR_MSG);

    return OptimizeStat.success(
        rawSql, List.of(optAst.toString(false)), List.of(optimizer.traceOf(optPlan)));
  }

  private static PlanContext parsePlan(String rawSql, String dbType, Schema schema) {
    try {
      final SqlNode ast = SqlSupport.parseSql(dbType, rawSql);
      if (ast == null || !PlanSupport.isSupported(ast)) return null;

      ast.context().setSchema(schema);
      normalizeAst(ast);

      final PlanContext plan = assemblePlan(ast, schema);
      if (plan == null) return null;

      return plan;
    } catch (Throwable ex) {
      return null;
    }
  }

  private static Properties mkDbProps(String dbType, String appName) {
    final String dbName = appName + "_" + "base";
    return DbSupport.dbProps(dbType, dbName);
  }
}
