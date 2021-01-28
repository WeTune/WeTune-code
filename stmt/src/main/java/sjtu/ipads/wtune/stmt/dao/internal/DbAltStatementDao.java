package sjtu.ipads.wtune.stmt.dao.internal;

import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.stmt.dao.AltStatementDao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static sjtu.ipads.wtune.stmt.dao.internal.DbStatementDao.*;

public class DbAltStatementDao extends DbDao implements AltStatementDao {
  private static final AltStatementDao INSTANCE = new DbAltStatementDao();

  private DbAltStatementDao() {}

  public static AltStatementDao instance() {
    return INSTANCE;
  }

  static final String KEY_KIND = "kind";

  private static final String SELECT_ITEMS =
      String.format(
          "stmt_app_name AS %s, stmt_id AS %s, stmt_trace AS %s, "
              + "stmt_alt_kind AS %s, alt_raw_sql AS %s ",
          KEY_APP_NAME, KEY_STMT_ID, KEY_STACK_TRACE, KEY_KIND, KEY_RAW_SQL);

  private static final String FIND_BY_STMT =
      "SELECT "
          + SELECT_ITEMS
          + " FROM wtune_stmts INNER JOIN wtune_alt_stmts"
          + " ON stmt_app_name = alt_app_name"
          + "  AND stmt_id = alt_stmt_id"
          + " WHERE stmt_app_name = ? AND stmt_id = ?";

  private static Statement toStatement(ResultSet rs) throws SQLException {
    return Statement.build(
        rs.getString(KEY_APP_NAME),
        rs.getInt(KEY_STMT_ID),
        rs.getString(KEY_KIND),
        rs.getString(KEY_RAW_SQL),
        rs.getString(KEY_STACK_TRACE));
  }

  @Override
  public List<Statement> findByStmt(String appName, int stmtId) {
    try {
      final PreparedStatement ps = prepare(FIND_BY_STMT);
      ps.setString(1, appName);
      ps.setInt(2, stmtId);
      final ResultSet rs = ps.executeQuery();

      final List<Statement> alts = new ArrayList<>(4);
      while (rs.next()) alts.add(toStatement(rs));

      return alts;

    } catch (SQLException throwables) {
      throw new RuntimeException(throwables);
    }
  }
}
