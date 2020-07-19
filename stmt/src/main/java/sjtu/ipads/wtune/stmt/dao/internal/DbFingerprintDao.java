package sjtu.ipads.wtune.stmt.dao.internal;

import sjtu.ipads.wtune.stmt.StmtException;
import sjtu.ipads.wtune.stmt.dao.FingerprintDao;
import sjtu.ipads.wtune.stmt.statement.OutputFingerprint;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class DbFingerprintDao extends DbDao implements FingerprintDao {
  public DbFingerprintDao(Supplier<Connection> connectionSupplier) {
    super(connectionSupplier);
  }

  public static final String KEY_APP_NAME = "appName";
  public static final String KEY_STMT_ID = "stmtId";
  public static final String KEY_POINT = "point";
  public static final String KEY_INDEX = "idx";
  public static final String KEY_HASH = "hash";

  private static final String SELECT_ITEM =
      String.format(
          "output_app_name AS %s, output_stmt_id AS %s, "
              + "output_input AS %s, output_index AS %s, output_hash AS %ss",
          KEY_APP_NAME, KEY_STMT_ID, KEY_POINT, KEY_INDEX, KEY_HASH);

  private static final String FIND_BY_STMT =
      "SELECT "
          + SELECT_ITEM
          + " FROM wtune_stmt_output"
          + " WHERE output_app_name = ? AND output_stmt_id = ?"
          + " ORDER BY output_input, output_index";

  private static final String UPSERT =
      "INSERT OR REPLACE INTO wtune_stmt_output "
          + "(output_app_name, output_stmt_id, output_input, output_index, output_hash) "
          + "VALUES (?, ?, ?, ?, ?)";

  @Override
  public List<OutputFingerprint> findByStmt(Statement stmt) {
    final PreparedStatement find = prepare(FIND_BY_STMT);
    try {
      final String appName = stmt.appName();
      final int stmtId = stmt.stmtId();

      find.setString(1, appName);
      find.setInt(2, stmtId);
      final ResultSet rs = find.executeQuery();

      final List<OutputFingerprint> ret = new ArrayList<>();

      OutputFingerprint fingerprint = null;
      List<Integer> hashes = null;
      int curInput = -1;

      while (rs.next()) {
        final int input = rs.getInt(KEY_POINT);
        if (curInput != input) {
          fingerprint = new OutputFingerprint();
          fingerprint.setAppName(appName);
          fingerprint.setStmtId(stmtId);
          fingerprint.setPoint(curInput = input);
          fingerprint.setHashes(hashes = new ArrayList<>());

          ret.add(fingerprint);
        }

        assert fingerprint != null;
        assert rs.getInt(KEY_INDEX) == hashes.size();

        hashes.add(rs.getInt(KEY_HASH));
      }

      return ret;

    } catch (SQLException throwables) {
      throw new StmtException(throwables);
    }
  }

  @Override
  public void save(OutputFingerprint fingerprint) {
    final PreparedStatement upsert = prepare(UPSERT);
    final String appName = fingerprint.appName();
    final int stmtId = fingerprint.stmtId();
    final int input = fingerprint.point();
    final List<Integer> hashes = fingerprint.hashes();
    try {
      for (int i = 0; i < hashes.size(); i++) {
        final Integer hash = hashes.get(i);
        upsert.setString(1, appName);
        upsert.setInt(2, stmtId);
        upsert.setInt(3, input);
        upsert.setInt(4, i);
        upsert.setInt(5, hash);
        upsert.addBatch();
      }
      upsert.executeUpdate();
    } catch (SQLException throwables) {
      throw new StmtException(throwables);
    }
  }
}
