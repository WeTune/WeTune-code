package sjtu.ipads.wtune.superopt.profiler;

import static sjtu.ipads.wtune.sqlparser.ast.ASTNode.MYSQL;
import static sjtu.ipads.wtune.sqlparser.ast.ASTNode.POSTGRESQL;

public interface CostQuery {
  double getCost();

  static CostQuery mysql(ConnectionProvider provider, String query) {
    return new MySQLCostQuery(provider, query);
  }

  static CostQuery pg(ConnectionProvider provider, String query) {
    return new PGCostQuery(provider, query);
  }

  static CostQuery make(String dbType, ConnectionProvider provider, String query) {
    switch (dbType) {
      case MYSQL:
        return mysql(provider, query);
      case POSTGRESQL:
        return pg(provider, query);
      default:
        throw new IllegalArgumentException("unknown db type");
    }
  }
}
