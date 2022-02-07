package sjtu.ipads.wtune.stmt.dao.internal;

import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.stmt.dao.OptStatementDao;
import sjtu.ipads.wtune.stmt.support.OptimizerType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class DbOptStatementDao extends DbDao implements OptStatementDao {

  public static OptStatementDao instance(OptimizerType kind) {
    return new DbOptStatementDao(kind);
  }

  static final String KEY_APP_NAME = "app";
  static final String KEY_STMT_ID = "stmtId";
  static final String KEY_RAW_SQL = "rawSql";

  private static final String SELECT_ITEMS =
      String.format(
          "opt_app_name AS %s, opt_stmt_id AS %s, opt_raw_sql AS %s ",
          KEY_APP_NAME, KEY_STMT_ID, KEY_RAW_SQL);

  private String OPT_STMTS_TABLE;
  private String FIND_ALL;
  private String FIND_ONE;
  private String FIND_BY_APP;

  // Update optimized stmts query
  private String CLEAN_OPT_STMT;
  private String ADD_OPT_STMTS;

  private DbOptStatementDao(OptimizerType kind) {
    switch (kind) {
      case WeTune -> OPT_STMTS_TABLE = "wtune_opt_stmts_wtune";
      case Spes -> OPT_STMTS_TABLE = "wtune_opt_stmts_spes";
      case Merge -> OPT_STMTS_TABLE = "wtune_opt_stmts_wtune_spes";
    }
    initQueryTemplates();
  }

  private void initQueryTemplates() {
    FIND_ALL = "SELECT " + SELECT_ITEMS + "FROM " + OPT_STMTS_TABLE + " ";
    FIND_ONE = FIND_ALL + "WHERE opt_app_name = ? AND opt_stmt_id = ?";
    FIND_BY_APP = FIND_ALL + "WHERE opt_app_name = ?";
    CLEAN_OPT_STMT = "DELETE FROM " + OPT_STMTS_TABLE + " WHERE TRUE";
    ADD_OPT_STMTS = "INSERT INTO " + OPT_STMTS_TABLE + " VALUES (?, ?, ?, ?)";
  }

  private static Statement toStatement(ResultSet rs) throws SQLException {
    final Statement stmt =
        Statement.mk(
            rs.getString(KEY_APP_NAME), rs.getInt(KEY_STMT_ID), rs.getString(KEY_RAW_SQL), null);
    stmt.setRewritten(true);
    return stmt;
  }

  @Override
  public Statement findOne(String appName, int stmtId) {
    try {
      final PreparedStatement ps = prepare(FIND_ONE);
      ps.setString(1, appName);
      ps.setInt(2, stmtId);

      final ResultSet rs = ps.executeQuery();

      if (rs.next()) return toStatement(rs);
      else return null;

    } catch (SQLException throwables) {
      throw new RuntimeException(throwables);
    }
  }

  @Override
  public List<Statement> findByApp(String appName) {
    try {
      final PreparedStatement ps = prepare(FIND_BY_APP);
      ps.setString(1, appName);

      final ResultSet rs = ps.executeQuery();

      final List<Statement> stmts = new ArrayList<>(250);
      while (rs.next()) stmts.add(toStatement(rs));

      return stmts;

    } catch (SQLException throwables) {
      throw new RuntimeException(throwables);
    }
  }

  @Override
  public List<Statement> findAll() {
    try {
      final PreparedStatement ps = prepare(FIND_ALL);
      final ResultSet rs = ps.executeQuery();

      final List<Statement> stmts = new ArrayList<>(10000);
      while (rs.next()) stmts.add(toStatement(rs));

      return stmts;

    } catch (SQLException throwables) {
      throw new RuntimeException(throwables);
    }
  }

  @Override
  public void cleanOptStmts() {
    try {
      final PreparedStatement clean0 = prepare(CLEAN_OPT_STMT);
      clean0.executeUpdate();
    } catch (SQLException throwables) {
      throw new RuntimeException(throwables);
    }
  }

  @Override
  public void updateOptStmts(Statement stmt) {
    try {
      final PreparedStatement insert0 = prepare(ADD_OPT_STMTS);

      insert0.setString(1, stmt.appName());
      insert0.setInt(2, stmt.stmtId());

      insert0.setString(3, stmt.rawSql());
      if (stmt.stackTrace() == null)
        insert0.setNull(4, Types.VARCHAR);
      else
        insert0.setString(4, stmt.stackTrace());

      insert0.executeUpdate();
    } catch (SQLException throwables) {
      throw new RuntimeException(throwables);
    }
  }
}
