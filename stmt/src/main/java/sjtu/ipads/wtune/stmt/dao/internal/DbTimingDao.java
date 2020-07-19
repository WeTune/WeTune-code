package sjtu.ipads.wtune.stmt.dao.internal;

import sjtu.ipads.wtune.stmt.StmtException;
import sjtu.ipads.wtune.stmt.dao.TimingDao;
import sjtu.ipads.wtune.stmt.statement.Statement;
import sjtu.ipads.wtune.stmt.statement.Timing;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static sjtu.ipads.wtune.stmt.statement.Timing.*;

public class DbTimingDao extends DbDao implements TimingDao {
  public DbTimingDao(Supplier<Connection> connectionSupplier) {
    super(connectionSupplier);
  }

  private static final String SELECT_ITEMS =
      String.format(
          "perf_app_name AS %s, perf_stmt_id AS %s, "
              + "perf_tag AS %s, perf_p50 AS %s, "
              + "perf_p90 AS %s, perf_P99 AS %s",
          KEY_APP_NAME, KEY_STMT_ID, KEY_TAG, KEY_P50, KEY_P90, KEY_P99);

  private static final String FIND_BY_STMT =
      "SELECT "
          + SELECT_ITEMS
          + " FROM wtune_stmt_perf"
          + " WHERE perf_app_name = ? AND perf_stmt_id = ?";

  private static final String FIND_ONE =
      "SELECT "
          + SELECT_ITEMS
          + " FROM wtune_stmt_perf"
          + " WHERE perf_app_name = ? AND perf_stmt_id = ? AND perf_tag = ?";

  private static final String INSERT_HISTORY =
      "INSERT INTO wtune_stmt_perf_history "
          + "(history_app_name, history_stmt_id, history_tag,"
          + " history_p50, history_p90, history_p99) "
          + "VALUES (?, ?, ?, ?, ?, ?)";

  private static final String UPSERT_PERF =
      "INSERT OR REPLACE INTO wtune_stmt_perf "
          + "(perf_app_name, perf_stmt_id, perf_tag,"
          + " perf_p50, perf_p90, perf_p99) "
          + "VALUES (?, ?, ?, ?, ?, ?)";

  private static Timing inflate(Timing timing, ResultSet rs) throws SQLException {
    return timing
        .setAppName(rs.getString(KEY_APP_NAME))
        .setStmtId(rs.getInt(KEY_STMT_ID))
        .setTag(rs.getString(KEY_TAG))
        .setP50(rs.getLong(KEY_P50))
        .setP90(rs.getLong(KEY_P90))
        .setP99(rs.getLong(KEY_P99));
  }

  @Override
  public List<Timing> findByStmt(Statement stmt) {
    final PreparedStatement ps = prepare(FIND_BY_STMT);
    try {
      ps.setString(1, stmt.appName());
      ps.setInt(2, stmt.stmtId());
      final ResultSet rs = ps.executeQuery();

      final List<Timing> timings = new ArrayList<>(3);
      while (rs.next()) timings.add(inflate(new Timing(), rs));

      return timings;

    } catch (SQLException throwables) {
      throw new StmtException(throwables);
    }
  }

  @Override
  public void insert(Timing timing) {
    final PreparedStatement find = prepare(FIND_ONE);
    final PreparedStatement insertHistory = prepare(INSERT_HISTORY);
    final PreparedStatement upsert = prepare(UPSERT_PERF);

    try {
      find.setString(1, timing.appName());
      find.setInt(2, timing.stmtId());
      find.setString(3, timing.tag());
      final ResultSet rs = find.executeQuery();
      if (rs.next()) {
        final Timing existing = inflate(new Timing(), rs);
        if (existing.p50() == timing.p50()
            && existing.p90() == timing.p90()
            && existing.p99() == timing.p99()) return;

        fillParamAndExec(existing, insertHistory);
      }

      fillParamAndExec(timing, upsert);

    } catch (SQLException throwables) {
      throwables.printStackTrace();
    }
  }

  private void fillParamAndExec(Timing timing, PreparedStatement upsert) throws SQLException {
    upsert.setString(1, timing.appName());
    upsert.setInt(2, timing.stmtId());
    upsert.setString(3, timing.tag());
    upsert.setLong(4, timing.p50());
    upsert.setLong(5, timing.p90());
    upsert.setLong(6, timing.p99());
    upsert.executeUpdate();
  }
}
