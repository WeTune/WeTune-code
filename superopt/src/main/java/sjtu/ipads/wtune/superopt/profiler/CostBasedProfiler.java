package sjtu.ipads.wtune.superopt.profiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.sql.DataSource;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;

public class CostBasedProfiler implements Profiler {
  private static final DataSourceFactory FACTORY = new DataSourceFactory();
  private final Properties connProps;
  private final DataSource dataSource;

  public CostBasedProfiler(Properties connProps) {
    this.connProps = connProps;
    this.dataSource = FACTORY.make(connProps);
  }

  @Override
  public List<ASTNode> pickOptimized(ASTNode baseline, List<ASTNode> candidates) {
    if (candidates.size() <= 1) return candidates;

    double minCost = getCost(baseline.toString());
    final List<ASTNode> optimized = new ArrayList<>();

    for (ASTNode query : candidates) {
      final double cost = getCost(query.toString());
      if (cost > minCost) continue;
      if (cost < minCost) optimized.clear();
      minCost = cost;
      optimized.add(query);
    }

    return optimized;
  }

  private double getCost(String query) {
    return CostQuery.of(connProps.getProperty("dbType"), dataSource::getConnection, query)
        .getCost();
  }
}
