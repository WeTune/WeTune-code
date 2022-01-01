package sjtu.ipads.wtune.superopt.daemon;

import sjtu.ipads.wtune.sqlparser.ast1.SqlNode;
import sjtu.ipads.wtune.stmt.Statement;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static sjtu.ipads.wtune.stmt.support.Workflow.parameterize;

public class RegistrationBase implements Registration {
  private static final long TTL = 50 * 60 * 1000; // 50 min

  private final Map<String, Status> registration = new ConcurrentHashMap<>();

  @Override
  public void register(Statement stmt, SqlNode optimized) {
    final String query = stmt.parsed().toString();

    if (optimized == null) {
      // remember a query that is unable to optimize
      registration.put(query, new Status(-1, System.currentTimeMillis() + TTL));
      return;
    }

    // assume the the original and optimized queries are both parameterize

    final Status existing = registration.get(query);
    if (existing != null) uninstall(existing.id);

    final int id = install(stmt.appName(), query, optimized.toString());
    if (id != -1) registration.put(query, new Status(id, System.currentTimeMillis() + TTL));
  }

  @Override
  public boolean contains(Statement stmt) {
    parameterize(stmt.parsed());
    final Status status = registration.get(stmt.parsed().toString());
    return status != null && status.expiration <= System.currentTimeMillis();
  }

  protected int install(String dbName, String originalQuery, String optimizedQuery) {
    return 0;
  }

  protected void uninstall(int id) {}

  private static class Status {
    private final int id;
    private final long expiration;

    private Status(int id, long expiration) {
      this.id = id;
      this.expiration = expiration;
    }
  }
}
