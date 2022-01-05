package sjtu.ipads.wtune.superopt.daemon;

import org.apache.commons.lang3.NotImplementedException;
import sjtu.ipads.wtune.sql.ast.SqlNode;
import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.superopt.profiler.ConnectionProvider;

import static sjtu.ipads.wtune.sql.ast.SqlNode.MySQL;
import static sjtu.ipads.wtune.sql.ast.SqlNode.PostgreSQL;

public interface Registration {
  void register(Statement stmt, SqlNode optimized);

  boolean contains(Statement stmt);

  static Registration make(String dbType, ConnectionProvider connPool) {
    switch (dbType) {
      case MySQL:
        return new MySQLRegistration(connPool);
      case PostgreSQL:
        throw new NotImplementedException();
      default:
        throw new IllegalArgumentException();
    }
  }
}
