package sjtu.ipads.wtune.stmt.internal;

import static java.util.function.Function.identity;
import static sjtu.ipads.wtune.sqlparser.ast.ASTNode.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.stmt.App;
import sjtu.ipads.wtune.stmt.dao.SchemaPatchDao;
import sjtu.ipads.wtune.stmt.utils.FileUtils;

public class AppImpl implements App {
  private final String name;
  private String dbType;
  private final Map<String, Schema> schemas;
  private Properties connProps;

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

  public Schema schema(String tag, boolean patched) {
    final Schema existing = schemas.get(tag);
    if (existing == null) {
      final Schema schema = readSchema(tag);
      if (patched) schema.patch(SchemaPatchDao.instance().findByApp(name));
      schemas.put(tag, schema);
      return schema;
    } else return existing;
  }

  @Override
  public Properties dbProps() {
    if (connProps == null) {
      connProps = new Properties();
      if (MYSQL.equals(dbType)) {
        connProps.setProperty(
            "jdbcUrl", "jdbc:mysql://10.0.0.103:3306/" + name + "_base?useSSL=false");
        connProps.setProperty("username", "root");
        connProps.setProperty("password", "admin");
        connProps.setProperty("dbType", MYSQL);

      } else if (POSTGRESQL.equals(dbType)) {
        connProps.setProperty("jdbcUrl", "jdbc:postgresql://10.0.0.103:5432/" + name + "_base");
        connProps.setProperty("username", "root");
        connProps.setProperty("dbType", POSTGRESQL);

      } else if (SQLSERVER.equals(dbType)){
        connProps.setProperty("jdbcUrl", "jdbc:sqlserver://10.0.0.103:1433;DatabaseName=" + name + "_base");
        connProps.setProperty("username", "SA");
        connProps.setProperty("dbType", SQLSERVER);
      } else throw new IllegalArgumentException("unknown db type");
    }

    return connProps;
  }

  @Override
  public void setDbType(String dbType) {
    this.dbType = dbType;
  }

  @Override
  public void setSchema(String tag, Schema schema) {
    schemas.put(tag, schema);
  }

  @Override
  public void setDbConnProps(Properties props) {
    this.connProps = props;
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
  private static final Set<String> SQLSERVER_APPS = Set.of(
//          "broadleaf", "diaspora", "discourse", "eladmin", "fatfreecrm", "febs", "forest_blog",
//          "gitlab", "guns", "halo", "homeland", "lobsters", "publiccms", "pybbs", "redmine",
//          "refinerycms", "sagan", "shopizer", "solidus", "spree"
  );

  private static final Map<String, App> KNOWN_APPS =
      Arrays.stream(APP_NAMES)
          .map(it -> new AppImpl(it, SQLSERVER_APPS.contains(it)? SQLSERVER :
                                          (PG_APPS.contains(it) ? POSTGRESQL : MYSQL)))
          .collect(Collectors.toMap(App::name, identity()));
}
