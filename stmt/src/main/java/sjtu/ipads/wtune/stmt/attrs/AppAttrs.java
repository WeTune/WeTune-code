package sjtu.ipads.wtune.stmt.attrs;

import sjtu.ipads.wtune.common.attrs.Attrs;
import sjtu.ipads.wtune.stmt.schema.Column;

import java.util.Set;

public interface AppAttrs {
  String APP_ATTR_PREFIX = "app.attr.";

  private static String attrPrefix(String name) {
    return APP_ATTR_PREFIX + name;
  }

  Attrs.Key<Set<Column>> IMPLIED_FOREIGN_KEYS =
      Attrs.key2(attrPrefix("actualForeignKeys"), Set.class);
}
