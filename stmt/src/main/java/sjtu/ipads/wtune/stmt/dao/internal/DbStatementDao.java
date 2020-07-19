package sjtu.ipads.wtune.stmt.dao.internal;

import sjtu.ipads.wtune.stmt.StmtException;
import sjtu.ipads.wtune.stmt.dao.StatementDao;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static sjtu.ipads.wtune.stmt.statement.Statement.*;

public class DbStatementDao extends DbDao implements StatementDao {
  public DbStatementDao(Supplier<Connection> connectionSupplier) {
    super(connectionSupplier);
  }

  private static final String SELECT_ITEMS =
      String.format(
          "stmt_app_name AS %s, stmt_id AS %s, stmt_raw_sql AS %s ",
          KEY_APP_NAME, KEY_STMT_ID, KEY_RAW_SQL);
  private static final String FIND_ALL = "SELECT " + SELECT_ITEMS + "FROM wtune_stmts ";
  private static final String FIND_ONE = FIND_ALL + "WHERE stmt_app_name = ? AND stmt_id = ?";
  private static final String FIND_BY_APP = FIND_ALL + "WHERE stmt_app_name = ?";
  private static final String DELETE_ONE =
      "DELETE FROM wtune_stmts WHERE stmt_app_name = ? AND stmt_id = ?";
  private static final String INSERT_DELETED =
      "INSERT INTO wtune_deleted_stmts (stmt_app_name, stmt_id, stmt_raw_sql, cause) "
          + "VALUES (?, ?, ?, ?)";

  private static Statement inflate(Statement stmt, ResultSet rs) throws SQLException {
    final String appName = rs.getString(KEY_APP_NAME);
    final int stmtId = rs.getInt(KEY_STMT_ID);
    final String rawSql = rs.getString(KEY_RAW_SQL);

    stmt.setAppName(appName);
    stmt.setStmtId(stmtId);
    stmt.setRawSql(rawSql);

    return stmt;
  }

  @Override
  public Statement findOne(String appName, int stmtId) {
    final PreparedStatement ps = prepare(FIND_ONE);
    try {
      ps.setString(1, appName);
      ps.setInt(2, stmtId);

      final ResultSet rs = ps.executeQuery();

      if (rs.next()) return inflate(new Statement(), rs);
      else return null;

    } catch (SQLException throwables) {
      throw new StmtException(throwables);
    }
  }

  @Override
  public List<Statement> findByApp(String appName) {
    final PreparedStatement ps = prepare(FIND_BY_APP);
    try {
      ps.setString(1, appName);

      final ResultSet rs = ps.executeQuery();

      final List<Statement> stmts = new ArrayList<>(250);
      while (rs.next()) stmts.add(inflate(new Statement(), rs));

      return stmts;

    } catch (SQLException throwables) {
      throw new StmtException(throwables);
    }
  }

  @Override
  public List<Statement> findAll() {
    final PreparedStatement ps = prepare(FIND_ALL);
    try {
      final ResultSet rs = ps.executeQuery();

      final List<Statement> stmts = new ArrayList<>(10000);
      while (rs.next()) stmts.add(inflate(new Statement(), rs));

      return stmts;

    } catch (SQLException throwables) {
      throw new StmtException(throwables);
    }
  }

  @Override
  public void delete(Statement stmt, String cause) {
    final PreparedStatement delete = prepare(DELETE_ONE);
    final PreparedStatement insert = prepare(INSERT_DELETED);
    try {
      insert.setString(1, stmt.appName());
      insert.setInt(2, stmt.stmtId());
      insert.setString(3, stmt.rawSql());
      insert.setString(4, cause);
      insert.executeUpdate();

      delete.setString(1, stmt.appName());
      delete.setInt(2, stmt.stmtId());
      delete.executeUpdate();

    } catch (SQLException throwables) {
      throw new StmtException(throwables);
    }
  }
}
