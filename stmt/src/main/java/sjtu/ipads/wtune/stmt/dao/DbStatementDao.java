package sjtu.ipads.wtune.stmt.dao;

import sjtu.ipads.wtune.stmt.StatementDao;
import sjtu.ipads.wtune.stmt.StmtException;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static sjtu.ipads.wtune.common.utils.Commons.threadLocal;
import static sjtu.ipads.wtune.stmt.statement.Statement.*;

public class DbStatementDao implements StatementDao {
  private final Supplier<Connection> connectionSupplier;
  private ThreadLocal<Connection> connection = new ThreadLocal<>();

  public DbStatementDao(Supplier<Connection> connectionSupplier) {
    this.connectionSupplier = connectionSupplier;
  }

  private static final String SELECT_ITEMS =
      String.format(
          "stmt_app_name AS %s, stmt_id AS %s, stmt_raw_sql AS %s ",
          KEY_APP_NAME, KEY_STMT_ID, KEY_RAW_SQL);

  private static final String FIND_ONE_SQL =
      "SELECT "
          + SELECT_ITEMS
          + "FROM wtune_stmts "
          + "WHERE stmt_app_name = ? "
          + "  AND stmt_id = ?";

  private static final String FIND_BY_APP_SQL =
      "SELECT " + SELECT_ITEMS + "FROM wtune_stmts WHERE stmt_app_name = ?";

  private static final String FIND_ALL_SQL = "SELECT " + SELECT_ITEMS + "FROM wtune_stmts";

  private final ThreadLocal<PreparedStatement> findOneCache = new ThreadLocal<>();
  private final ThreadLocal<PreparedStatement> findByAppCache = new ThreadLocal<>();
  private final ThreadLocal<PreparedStatement> findAllCache = new ThreadLocal<>();

  private Connection connection() {
    return threadLocal(connection, connectionSupplier);
  }

  private void closeConnection() throws SQLException {
    findOneCache.remove();
    findByAppCache.remove();
    findAllCache.remove();

    final Connection conn = connection.get();
    if (conn == null) return;
    conn.close();
  }

  private PreparedStatement prepare(String sql) {
    final Connection conn = connection();
    try {
      return conn.prepareStatement(sql);
    } catch (SQLException throwables) {
      throw new StmtException(throwables);
    }
  }

  private PreparedStatement findOne0() {
    return threadLocal(findOneCache, () -> prepare(FIND_ONE_SQL));
  }

  private PreparedStatement findByApp0() {
    return threadLocal(findByAppCache, () -> prepare(FIND_BY_APP_SQL));
  }

  private PreparedStatement findAll0() {
    return threadLocal(findAllCache, () -> prepare(FIND_ALL_SQL));
  }

  private static Statement inflate(ResultSet rs) throws SQLException {
    final String appName = rs.getString(KEY_APP_NAME);
    final int stmtId = rs.getInt(KEY_STMT_ID);
    final String rawSql = rs.getString(KEY_RAW_SQL);

    final Statement stmt = new Statement();
    stmt.setAppName(appName);
    stmt.setStmtId(stmtId);
    stmt.setRawSql(rawSql);

    return stmt;
  }

  @Override
  public Statement findOne(String appName, int stmtId) {
    final PreparedStatement ps = findOne0();
    try {
      ps.setString(1, appName);
      ps.setInt(2, stmtId);

      final ResultSet rs = ps.executeQuery();

      if (rs.next()) return inflate(rs);
      else return null;

    } catch (SQLException throwables) {
      throw new StmtException(throwables);
    }
  }

  @Override
  public List<Statement> findByApp(String appName) {
    final PreparedStatement ps = findByApp0();
    try {
      ps.setString(1, appName);

      final ResultSet rs = ps.executeQuery();

      final List<Statement> stmts = new ArrayList<>(250);
      while (rs.next()) stmts.add(inflate(rs));

      return stmts;

    } catch (SQLException throwables) {
      throw new StmtException(throwables);
    }
  }

  @Override
  public List<Statement> findAll() {
    final PreparedStatement ps = findAll0();
    try {
      final ResultSet rs = ps.executeQuery();

      final List<Statement> stmts = new ArrayList<>(10000);
      while (rs.next()) stmts.add(inflate(rs));

      return stmts;

    } catch (SQLException throwables) {
      throw new StmtException(throwables);
    }
  }

  @Override
  public void close() {
    try {
      closeConnection();
    } catch (SQLException throwables) {
      throw new StmtException(throwables);
    }
  }
}
