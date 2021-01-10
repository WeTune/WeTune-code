package sjtu.ipads.wtune.stmt;

import sjtu.ipads.wtune.stmt.internal.AppImpl;
import sjtu.ipads.wtune.stmt.schema.Schema;

import java.util.Collection;
import java.util.List;

public interface App {
  String name();

  String dbType();

  Schema schema(String tag);

  List<Timing> timing(String tag);

  static App find(String name) {
    return AppImpl.find(name);
  }

  static App build(String name, String dbType) {
    return AppImpl.build(name, dbType);
  }

  static Collection<App> all() {
    return AppImpl.all();
  }
}
