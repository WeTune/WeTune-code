package sjtu.ipads.wtune.superopt.profiler;

import static sjtu.ipads.wtune.sqlparser.ast.ASTNode.*;

public interface CostQuery {
  double getCost();

  static CostQuery mysql(ConnectionProvider provider, String query) {
    return new MySQLCostQuery(provider, query);
  }

  static CostQuery pg(ConnectionProvider provider, String query) {
    return new PGCostQuery(provider, query);
  }

  static CostQuery sqlserver(ConnectionProvider provider, String query) {
    return new SQLServerCostQuery(provider, query);
  }

  static CostQuery mk(String dbType, ConnectionProvider provider, String query) {
    return switch (dbType) {
      case MYSQL -> mysql(provider, query);
      case POSTGRESQL -> pg(provider, query);
      case SQLSERVER -> sqlserver(provider, query);
      default -> throw new IllegalArgumentException("unknown db type");
    };
  }
}
