package sjtu.ipads.wtune.stmt.dao;

import sjtu.ipads.wtune.stmt.StmtException;
import sjtu.ipads.wtune.stmt.dao.internal.StatementDaoInstance;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Supplier;

public interface StatementDao {
  Statement findOne(String appName, int stmtId);

  List<Statement> findByApp(String appName);

  List<Statement> findAll();

  static Supplier<Connection> connectionSupplier(String url) {
    return () -> {
      try {
        return DriverManager.getConnection(url);
      } catch (SQLException throwables) {
        throw new StmtException(throwables);
      }
    };
  }

  default void registerAsGlobal() {
    StatementDaoInstance.register(this);
  }
}
