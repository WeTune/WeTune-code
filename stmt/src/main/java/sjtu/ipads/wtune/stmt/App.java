package sjtu.ipads.wtune.stmt;

import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.stmt.internal.AppImpl;

import java.util.Collection;

/**
 * Basic information about an application. Also serve as the reader and cache for per-app
 * information (e.g. statements, schema, timing)
 */
public interface App {
  String name();

  String dbType();

  Schema schema(String tag, boolean patched);

  default Schema schema(String tag) {
    return schema(tag, false);
  }

  static App of(String name) {
    return AppImpl.of(name);
  }

  static Collection<App> all() {
    return AppImpl.all();
  }
}
