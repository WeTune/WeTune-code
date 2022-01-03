package sjtu.ipads.wtune.superopt.daemon;

import org.apache.commons.lang3.NotImplementedException;
import sjtu.ipads.wtune.sql.ast1.SqlNode;
import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.superopt.profiler.ConnectionProvider;

import static sjtu.ipads.wtune.sql.ast.ASTNode.MYSQL;
import static sjtu.ipads.wtune.sql.ast.ASTNode.POSTGRESQL;

public interface Registration {
  void register(Statement stmt, SqlNode optimized);

  boolean contains(Statement stmt);

  static Registration make(String dbType, ConnectionProvider connPool) {
    switch (dbType) {
      case MYSQL:
        return new MySQLRegistration(connPool);
      case POSTGRESQL:
        throw new NotImplementedException();
      default:
        throw new IllegalArgumentException();
    }
  }
}
