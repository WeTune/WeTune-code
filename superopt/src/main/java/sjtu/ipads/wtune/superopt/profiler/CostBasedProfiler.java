package sjtu.ipads.wtune.superopt.profiler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import javax.sql.DataSource;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;

public class CostBasedProfiler implements Profiler {
  private final Properties connProps;
  private final DataSource dataSource;

  public CostBasedProfiler(Properties connProps) {
    this.connProps = connProps;
    this.dataSource = DataSourceFactory.instance().make(connProps);
  }

  @Override
  public List<ASTNode> pickOptimized(ASTNode baseline, List<ASTNode> candidates) {
    if (candidates.size() <= 1) return candidates;

    final double baselineCost = getCost(baseline.toString());
    double minCost = baselineCost;
    final List<ASTNode> optimized = new ArrayList<>();

    for (ASTNode query : candidates) {
      final double cost = getCost(query.toString());
      if (cost > minCost) continue;
      if (cost < minCost) optimized.clear();
      minCost = cost;
      optimized.add(query);
    }

    if (minCost <= baselineCost * 0.9) return optimized;
    else return Collections.emptyList();
  }

  private double getCost(String query) {
    return CostQuery.of(connProps.getProperty("dbType"), dataSource::getConnection, query)
        .getCost();
  }
}
