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

  void delete(Statement stmt, String cause);

  static Supplier<Connection> connectionSupplier(String url) {
    return new ConnectionHolder(url)::get;
  }

  default void registerAsGlobal() {
    StatementDaoInstance.register(this);
  }

  class ConnectionHolder {
    private final String url;
    private Connection connection;

    public ConnectionHolder(String url) {
      this.url = url;
      Runtime.getRuntime()
          .addShutdownHook(
              new Thread(
                  () -> {
                    try {
                      if (connection != null && !connection.isClosed()) connection.close();
                    } catch (SQLException throwables) {
                      throwables.printStackTrace();
                    }
                  }));
    }

    public Connection get() {
      try {
        if (connection == null || connection.isClosed())
          connection = DriverManager.getConnection(url);
      } catch (SQLException throwables) {
        throw new StmtException(throwables);
      }
      return connection;
    }
  }
}
