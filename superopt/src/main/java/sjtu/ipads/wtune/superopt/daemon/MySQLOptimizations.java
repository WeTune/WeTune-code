package sjtu.ipads.wtune.superopt.daemon;

import static sjtu.ipads.wtune.superopt.daemon.DaemonContext.LOG;

import java.lang.System.Logger.Level;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

public class MySQLOptimizations extends OptimizationsBase {
  private final DataSource dataSource;

  public MySQLOptimizations(String database, DataSource dataSource) {
    super(database);
    this.dataSource = dataSource;
  }

  @Override
  protected void uninstall(int id) {
    try (final Connection conn = dataSource.getConnection()) {
      conn.createStatement()
          .executeUpdate("DELETE FROM query_rewrite.rewrite_rules WHERE id=" + id);

    } catch (SQLException ex) {
      LOG.log(Level.WARNING, "failed to uninstall optimization for MySQL", ex);
    }
  }

  @Override
  protected int install(String originalQuery, String optimizedQuery) {
    try (final Connection conn = dataSource.getConnection()) {
      final Statement stmt = conn.createStatement();
      final String sql =
          "INSERT INTO query_rewrite.rewrite_rules (pattern_database, pattern, replacement) VALUES ('%s','%s','%s')";
      stmt.executeUpdate(
          sql.formatted(database, originalQuery, optimizedQuery), Statement.RETURN_GENERATED_KEYS);

      final ResultSet idResult = stmt.getGeneratedKeys();
      if (idResult.next()) {
        final int id = idResult.getInt(1);
        stmt.execute("CALL query_rewrite.flush_rewrite_rules();");
        return id;
      }

      return -1;

    } catch (SQLException ex) {
      LOG.log(Level.WARNING, "failed to install optimization for MySQL", ex);
      return -1;
    }
  }
}
