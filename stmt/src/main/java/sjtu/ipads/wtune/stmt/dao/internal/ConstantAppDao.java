package sjtu.ipads.wtune.stmt.dao.internal;

import sjtu.ipads.wtune.stmt.App;
import sjtu.ipads.wtune.stmt.dao.AppDao;

import java.util.Set;

import static sjtu.ipads.wtune.sqlparser.ast.SQLNode.MYSQL;
import static sjtu.ipads.wtune.sqlparser.ast.SQLNode.POSTGRESQL;

public class ConstantAppDao implements AppDao {
  private static final Set<String> PG_APPS = Set.of("gitlab", "homeland", "discourse");

  public App findOne(String name) {
    if (PG_APPS.contains(name)) return App.build(name, POSTGRESQL);
    else return App.build(name, MYSQL);
  }
}
