package sjtu.ipads.wtune.stmt.internal;

import sjtu.ipads.wtune.stmt.App;
import sjtu.ipads.wtune.stmt.Setup;
import sjtu.ipads.wtune.stmt.StmtException;
import sjtu.ipads.wtune.stmt.Timing;
import sjtu.ipads.wtune.stmt.dao.AppDao;
import sjtu.ipads.wtune.stmt.dao.SchemaDao;
import sjtu.ipads.wtune.stmt.schema.Schema;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class AppImpl implements App {
  private static final String[] KNOWN_APP_NAMES = {
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
  private static final Map<String, App> KNOWN_APPS = new HashMap<>();

  private final String name;
  private final String dbType;
  private final Map<String, Schema> schemas;

  private AppImpl(String name, String dbType) {
    this.name = name;
    this.dbType = dbType;
    this.schemas = new HashMap<>();
  }

  public static App find(String name) {
    return KNOWN_APPS.computeIfAbsent(name, AppDao.instance()::findOne);
  }

  public static App build(String name, String dbType) {
    return new AppImpl(name, dbType);
  }

  public static Collection<App> all() {
    Arrays.stream(KNOWN_APP_NAMES).forEach(AppImpl::find);
    return KNOWN_APPS.values();
  }

  public String name() {
    return name;
  }

  public String dbType() {
    return dbType;
  }

  @Override
  public Schema schema(String tag) {
    return schemas.computeIfAbsent(tag, t -> SchemaDao.instance().findOne(name(), t, dbType()));
  }

  @Override
  public List<Timing> timing(String tag) {
    try {
      final Path path = Setup.current().outputDir().resolve(name).resolve("eval." + tag);
      if (!path.toFile().exists()) return Collections.emptyList();

      return Files.lines(path)
          .map(Timing::fromLine)
          .peek(it -> it.setAppName(name).setTag(tag))
          .collect(Collectors.toList());

    } catch (IOException e) {
      throw new StmtException(e);
    }
  }
}
