package sjtu.ipads.wtune.stmt.dao.internal;

import sjtu.ipads.wtune.stmt.StmtException;
import sjtu.ipads.wtune.stmt.dao.AltStatementDao;
import sjtu.ipads.wtune.stmt.statement.AltStatement;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static sjtu.ipads.wtune.stmt.statement.AltStatement.KEY_KIND;
import static sjtu.ipads.wtune.stmt.statement.AltStatement.KEY_RAW_SQL;

public class DbAltStatementDao extends DbDao implements AltStatementDao {
  public DbAltStatementDao(Supplier<Connection> connectionSupplier) {
    super(connectionSupplier);
  }

  private static final String SELECT_ITEMS =
      String.format("alt_kind AS %s, alt_raw_sql AS %s", KEY_KIND, KEY_RAW_SQL);

  private static final String FIND_BY_STMT =
      "SELECT "
          + SELECT_ITEMS
          + " FROM wtune_alt_stmts"
          + " WHERE alt_app_name = ? AND alt_stmt_id = ?";

  private static AltStatement inflate(Statement main, ResultSet rs) throws SQLException {
    final AltStatement alt = new AltStatement(main);
    alt.setKind(rs.getString(KEY_KIND));
    alt.setRawSql(rs.getString(KEY_RAW_SQL));
    return alt;
  }

  @Override
  public List<AltStatement> findByStmt(Statement stmt) {
    final PreparedStatement ps = prepare(FIND_BY_STMT);
    try {
      ps.setString(1, stmt.appName());
      ps.setInt(2, stmt.stmtId());
      final ResultSet rs = ps.executeQuery();

      final List<AltStatement> alts = new ArrayList<>(4);
      while (rs.next()) alts.add(inflate(stmt, rs));

      return alts;

    } catch (SQLException throwables) {
      throw new StmtException(throwables);
    }
  }
}
