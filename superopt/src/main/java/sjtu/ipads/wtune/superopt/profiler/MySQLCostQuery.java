package sjtu.ipads.wtune.superopt.profiler;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class MySQLCostQuery extends CostQueryBase {
  private static final String LABEL = "\"query_cost\": \"";

  public MySQLCostQuery(ConnectionProvider provider, String query) {
    super(provider, query);
  }

  @Override
  protected double doQuery() throws SQLException {
    try (final Connection conn = provider.get()) {
      final Statement stmt = conn.createStatement();
      final ResultSet rs = stmt.executeQuery("EXPLAIN FORMAT=JSON (" + query + ");");

      if (rs.next()) {
        final String json = rs.getString(1);
        final int idx = json.indexOf(LABEL);
        if (idx == -1) return Double.MAX_VALUE;

        final int start = idx + LABEL.length();
        final int end = json.indexOf("\"", start);
        return end == -1 ? Double.MAX_VALUE : Double.parseDouble(json.substring(start, end));

      } else return Double.MAX_VALUE;
    }
  }
}
