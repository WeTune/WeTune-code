package sjtu.ipads.wtune.stmt.dao.internal;

import sjtu.ipads.wtune.stmt.StmtException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static sjtu.ipads.wtune.common.utils.Commons.threadLocal;

public abstract class DbDao {
  private final Supplier<Connection> connectionSupplier;
  private final ThreadLocal<Connection> connection = new ThreadLocal<>();
  private final Map<String, ThreadLocal<PreparedStatement>> caches = new HashMap<>();

  public DbDao(Supplier<Connection> connectionSupplier) {
    this.connectionSupplier = connectionSupplier;
  }

  protected Connection connection() {
    return threadLocal(connection, connectionSupplier);
  }

  protected PreparedStatement prepare(String sql) {
    final ThreadLocal<PreparedStatement> threadLocal =
        caches.computeIfAbsent(sql, ignored -> new ThreadLocal<>());
    PreparedStatement ps = threadLocal.get();
    if (ps != null) return ps;

    final Connection conn = connection();
    try {
      threadLocal.set(ps = conn.prepareStatement(sql));
      return ps;
    } catch (SQLException throwables) {
      throw new StmtException(throwables);
    }
  }
}
