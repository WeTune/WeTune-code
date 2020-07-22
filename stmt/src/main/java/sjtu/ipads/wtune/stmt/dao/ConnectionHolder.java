package sjtu.ipads.wtune.stmt.dao;

import sjtu.ipads.wtune.stmt.StmtException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.function.Supplier;

public class ConnectionHolder {
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

  public static Supplier<Connection> supplier(String url) {
    return new ConnectionHolder(url)::get;
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
