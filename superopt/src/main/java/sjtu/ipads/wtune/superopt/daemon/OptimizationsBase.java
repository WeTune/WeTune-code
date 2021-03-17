package sjtu.ipads.wtune.superopt.daemon;

import static sjtu.ipads.wtune.stmt.support.Workflow.parameterize;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.stmt.Statement;

public class OptimizationsBase implements Optimizations {
  private static final long TTL = 50 * 60 * 1000; // 50 min

  protected final String database;
  private final Map<String, Status> registration = new ConcurrentHashMap<>();

  public OptimizationsBase(String database) {
    this.database = database;
  }

  @Override
  public void register(Statement stmt, ASTNode optimized) {
    // assume the the original and optimized queries are both parameterize
    final String query = stmt.parsed().toString();

    final Status existing = registration.get(query);
    if (existing != null) uninstall(existing.id);

    final int id = install(query, optimized.toString());
    if (id != -1) registration.put(query, new Status(id, System.currentTimeMillis() + TTL));
  }

  @Override
  public boolean contains(Statement stmt) {
    parameterize(stmt.parsed());
    final Status status = registration.get(stmt.parsed().toString());
    return status != null && status.expiration <= System.currentTimeMillis();
  }

  protected int install(String originalQuery, String optimizedQuery) {
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
