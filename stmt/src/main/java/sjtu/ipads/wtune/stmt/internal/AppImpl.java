package sjtu.ipads.wtune.stmt.internal;

import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.stmt.App;
import sjtu.ipads.wtune.stmt.utils.FileUtils;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;
import static sjtu.ipads.wtune.sqlparser.ast.ASTNode.MYSQL;
import static sjtu.ipads.wtune.sqlparser.ast.ASTNode.POSTGRESQL;

public class AppImpl implements App {
  private final String name;
  private final String dbType;

  private final Map<String, Schema> schemas;

  private AppImpl(String name, String dbType) {
    this.name = name;
    this.dbType = dbType;
    this.schemas = new HashMap<>();
  }

  public static App of(String name) {
    return KNOWN_APPS.computeIfAbsent(name, it -> new AppImpl(it, MYSQL));
  }

  public static Collection<App> all() {
    return KNOWN_APPS.values();
  }

  public String name() {
    return name;
  }

  public String dbType() {
    return dbType;
  }

  public Schema schema(String tag) {
    return schemas.computeIfAbsent(tag, this::readSchema);
  }

  private Schema readSchema(String tag) {
    final String str = FileUtils.readFile("schemas", name + "." + tag + ".schema.sql");
    return Schema.parse(dbType, str);
  }

  private static final String[] APP_NAMES = {
    "broadleaf",
    "diaspora",
    "discourse",
    "eladmin",
    "fatfreecrm",
    "febs",
    "forest_blog",
    "gitlab",
    "guns",
    "halo",
    "homeland",
    "lobsters",
    "publiccms",
    "pybbs",
    "redmine",
    "refinerycms",
    "sagan",
    "shopizer",
    "solidus",
    "spree"
  };

  private static final Set<String> PG_APPS = Set.of("discourse", "gitlab", "homeland");

  private static final Map<String, App> KNOWN_APPS =
      Arrays.stream(APP_NAMES)
          .map(it -> new AppImpl(it, PG_APPS.contains(it) ? POSTGRESQL : MYSQL))
          .collect(Collectors.toMap(App::name, identity()));
}
