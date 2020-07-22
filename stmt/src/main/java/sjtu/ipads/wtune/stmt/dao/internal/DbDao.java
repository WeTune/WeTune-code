package sjtu.ipads.wtune.stmt.dao.internal;

import sjtu.ipads.wtune.stmt.StmtException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public abstract class DbDao {
  private final Supplier<Connection> connectionSupplier;
  private final Map<String, PreparedStatement> caches = new HashMap<>();
  private Connection connection = null;

  public DbDao(Supplier<Connection> connectionSupplier) {
    this.connectionSupplier = connectionSupplier;
  }

  protected Connection connection() {
    if (connection == null) connection = connectionSupplier.get();
    return connection;
  }

  protected PreparedStatement prepare(String sql) throws SQLException {
    PreparedStatement ps = caches.get(sql);
    if (ps != null) return ps;

    final Connection conn = connection();
    caches.put(sql, ps = conn.prepareStatement(sql));
    return ps;
  }

  protected void begin() {
    try {
      final Connection conn = connection();
      conn.setAutoCommit(false);
    } catch (SQLException throwables) {
      throw new StmtException(throwables);
    }
  }

  protected void commit() {
    try {
      final Connection conn = connection();
      conn.commit();
      conn.setAutoCommit(true);
      //      conn.close();
      //      this.connection = null;
      //      caches.clear();
    } catch (SQLException throwables) {
      throw new StmtException(throwables);
    }
  }
}
