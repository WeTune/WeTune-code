package sjtu.ipads.wtune.sqlparser.rel;

import sjtu.ipads.wtune.sqlparser.rel.internal.SchemaImpl;

import java.util.Collection;

public interface Schema {
  Collection<? extends Table> tables();

  Table table(String name);

  static Schema parse(String dbType, String str) {
    return SchemaImpl.build(dbType, str);
  }
}
