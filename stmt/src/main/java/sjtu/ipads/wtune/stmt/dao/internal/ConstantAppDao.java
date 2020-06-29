package sjtu.ipads.wtune.stmt.dao.internal;

import sjtu.ipads.wtune.stmt.context.AppContext;
import sjtu.ipads.wtune.stmt.dao.AppDao;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static sjtu.ipads.wtune.sqlparser.SQLNode.MYSQL;
import static sjtu.ipads.wtune.sqlparser.SQLNode.POSTGRESQL;

public class ConstantAppDao implements AppDao {
  private static final Set<String> PG_APPS = Set.of("gitlab", "homeland", "discourse");

  @Override
  public AppContext inflateOne(AppContext ctx) {
    if (PG_APPS.contains(ctx.name())) ctx.setDbType(POSTGRESQL);
    else ctx.setDbType(MYSQL);
    return ctx;
  }
}
