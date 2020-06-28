package sjtu.ipads.wtune.stmt.dao;

import sjtu.ipads.wtune.stmt.StmtException;
import sjtu.ipads.wtune.stmt.dao.internal.DbStatementDao;
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

  void close();

  static StatementDao fromDb(Supplier<Connection> connectionSupplier) {
    return new DbStatementDao(connectionSupplier);
  }

  static Supplier<Connection> connectionSupplier(String url) {
    return () -> {
      try {
        return DriverManager.getConnection(url);
      } catch (SQLException throwables) {
        throw new StmtException(throwables);
      }
    };
  }

  static StatementDao getGlobal() {
    return StatementDaoInstance.instance();
  }

  default void registerAsGlobal() {
    StatementDaoInstance.register(this);
  }
}
