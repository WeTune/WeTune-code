package wtune.superopt.profiler;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import wtune.sql.ast.SqlNode;
import wtune.sql.plan.PlanContext;
import wtune.sql.util.ParamInterpolator;
import wtune.superopt.util.Complexity;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static wtune.common.datasource.DbSupport.MySQL;
import static wtune.common.datasource.DbSupport.SQLServer;
import static wtune.common.datasource.SQLSyntaxAdaptor.adaptToSQLServer;
import static wtune.sql.plan.PlanSupport.translateAsAst;

class ProfilerImpl implements Profiler {
  private final Properties dbProps;
  private PlanContext baseline;
  private double baseCost;
  private final List<PlanContext> plans;
  private final TDoubleList costs;

  ProfilerImpl(Properties dbProps) {
    this.dbProps = dbProps;
    this.plans = new ArrayList<>();
    this.costs = new TDoubleArrayList();
  }

  @Override
  public void setBaseline(PlanContext baseline) {
    plans.clear();
    costs.clear();
    this.baseline = baseline;
    this.baseCost = queryCost(translateAsAst(baseline, baseline.root(), false));
  }

  @Override
  public PlanContext getBaseline() {
    return baseline;
  }

  @Override
  public void profile(PlanContext plan) {
    if (plan == null) {
      plans.add(null);
      costs.add(Double.MAX_VALUE);
      return;
    }

    final SqlNode ast = translateAsAst(plan, plan.root(), false);
    final double cost = queryCost(ast);

    plans.add(plan);
    costs.add(cost);
  }

  @Override
  public PlanContext getPlan(int index) {
    return plans.get(index);
  }

  @Override
  public double getCost(int index) {
    return costs.get(index);
  }

  @Override
  public double getBaselineCost() {
    if (baseline == null) return Double.MAX_VALUE;
    return baseCost;
  }

  @Override
  public int minCostIndex() {
    double minCost = baseCost;
    int minCostIndex = -1;
    for (int i = 0, bound = costs.size(); i < bound; ++i) {
      if (costs.get(i) < minCost) {
        minCost = costs.get(i);
        minCostIndex = i;
      }
    }

    // MySQL doesn't correctly estimate some simplification (e.g. remove JOIN),
    // so let's do it ourself.
    if (minCostIndex == -1 && MySQL.equals(dbProps.getProperty("dbType"))) {
      Complexity minComplexity = Complexity.mk(baseline, baseline.root());
      for (int i = 0, bound = plans.size(); i < bound; i++) {
        final PlanContext plan = plans.get(i);
        if (plan == null) continue;
        final Complexity complexity = Complexity.mk(plan, plan.root());
        if (minComplexity.compareTo(complexity, false) > 0) {
          minComplexity = complexity;
          minCostIndex = i;
        }
      }
    }

    return minCostIndex;
  }

  @Override
  public int minCostIndexOfCandidates() {
    if (costs.size() == 0) return -1;

    double minCost = costs.get(0);
    int minCostIndex = 0;
    for (int i = 0, bound = costs.size(); i < bound; ++i) {
      if (costs.get(i) < minCost) {
        minCost = costs.get(i);
        minCostIndex = i;
      }
    }
    return minCostIndex;
  }

  private double queryCost(SqlNode ast) {
    final ParamInterpolator interpolator = new ParamInterpolator(ast);
    interpolator.go();

    String query = ast.toString();

    interpolator.undo();

    final String dbType = dbProps.getProperty("dbType");
    if (SQLServer.equals(dbType)) query = adaptToSQLServer(query);

    final DataSource dataSource = DataSourceFactory.instance().mk(dbProps);
    return CostQuery.mk(dbType, dataSource::getConnection, query).getCost();
  }

}
