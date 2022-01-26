package sjtu.ipads.wtune.stmt.dao.internal;

import sjtu.ipads.wtune.stmt.CalciteStmtProfile;
import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.stmt.StmtProfile;
import sjtu.ipads.wtune.stmt.dao.CalciteOptStatementDao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CalciteDbOptStatementDao extends DbDao implements CalciteOptStatementDao {
  private static final CalciteOptStatementDao INSTANCE = new CalciteDbOptStatementDao();

  private CalciteDbOptStatementDao() {}

  public static CalciteOptStatementDao instance() {
    return INSTANCE;
  }

  static final String KEY_APP_NAME = "app";
  static final String KEY_STMT_ID = "stmtId";
  static final String KEY_RAW_SQL = "rawSql";

  private static final String SELECT_ITEMS =
      String.format(
          "opt_app_name AS %s, opt_stmt_id AS %s, opt_raw_sql AS %s ",
          KEY_APP_NAME, KEY_STMT_ID, KEY_RAW_SQL);
  private static final String OPT_STMTS_TABLE = "calcite_opt_stmts";
  private static final String FIND_ALL = "SELECT " + SELECT_ITEMS + "FROM " + OPT_STMTS_TABLE + " ";
  private static final String FIND_ONE = FIND_ALL + "WHERE opt_app_name = ? AND opt_stmt_id = ?";
  private static final String FIND_BY_APP = FIND_ALL + "WHERE opt_app_name = ?";

  // Update profile query
  private static final String CLEAN_OPT_DATA =
      "UPDATE "
          + OPT_STMTS_TABLE
          + " SET p50_improve_q0 = null, p90_improve_q0 = null, p99_improve_q0 = null,"
          + " p50_improve_q1 = null, p90_improve_q1 = null, p99_improve_q1 = null"
          + " WHERE TRUE";

  private static final String UPDATE_OPT_DATA =
      "UPDATE "
          + OPT_STMTS_TABLE
          + " SET p50_improve_q0 = ?, p90_improve_q0 = ?, p99_improve_q0 = ?,"
          + " p50_improve_q1 = ?, p90_improve_q1 = ?, p99_improve_q1 = ?"
          + " WHERE opt_app_name = ? and opt_stmt_id = ?";

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
  public void cleanProfileData() {
    try {
      final PreparedStatement clean0 = prepare(CLEAN_OPT_DATA);
      clean0.executeUpdate();
    } catch (SQLException throwables) {
      throw new RuntimeException(throwables);
    }
  }

  @Override
  public void updateProfile(CalciteStmtProfile stmtProfile) {
    try {
      final PreparedStatement insert0 = prepare(UPDATE_OPT_DATA);
      insert0.setFloat(1, stmtProfile.p50ImproveQ0());
      insert0.setFloat(2, stmtProfile.p90ImproveQ0());
      insert0.setFloat(3, stmtProfile.p99ImproveQ0());
      insert0.setFloat(4, stmtProfile.p50ImproveQ1());
      insert0.setFloat(5, stmtProfile.p90ImproveQ1());
      insert0.setFloat(6, stmtProfile.p99ImproveQ1());
      insert0.setString(7, stmtProfile.appName());
      insert0.setInt(8, stmtProfile.stmtId());
      insert0.executeUpdate();

    } catch (SQLException throwables) {
      throw new RuntimeException(throwables);
    }
  }

}
