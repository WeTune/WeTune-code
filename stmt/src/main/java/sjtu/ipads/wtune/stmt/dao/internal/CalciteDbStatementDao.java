package sjtu.ipads.wtune.stmt.dao.internal;

import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.stmt.dao.CalciteStatementDao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CalciteDbStatementDao extends DbDao implements CalciteStatementDao {
  private static final CalciteStatementDao INSTANCE = new CalciteDbStatementDao();

  private CalciteDbStatementDao() {}

  public static CalciteStatementDao instance() {
    return INSTANCE;
  }

  static final String KEY_APP_NAME = "app";
  static final String KEY_STMT_ID = "stmtId";
  static final String KEY_RAW_SQL = "rawSql";

  private static final String SELECT_ITEMS =
      String.format(
          "stmt_app_name AS %s, stmt_id AS %s, stmt_raw_sql AS %s ",
          KEY_APP_NAME, KEY_STMT_ID, KEY_RAW_SQL);
  private static final String CALCITE_STMTS_TABLE = "calcite_stmts";
  private static final String FIND_ALL =
      "SELECT " + SELECT_ITEMS + "FROM " + CALCITE_STMTS_TABLE + " ";
  private static final String FIND_ONE = FIND_ALL + "WHERE stmt_app_name = ? AND stmt_id = ?";
  private static final String FIND_BY_APP = FIND_ALL + "WHERE stmt_app_name = ?";

  private static Statement toStatement(ResultSet rs) throws SQLException {
    return Statement.mk(
        rs.getString(KEY_APP_NAME),
        rs.getInt(KEY_STMT_ID),
        rs.getString(KEY_RAW_SQL),
        null);
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
}
