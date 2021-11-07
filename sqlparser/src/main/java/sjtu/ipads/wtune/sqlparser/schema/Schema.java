package sjtu.ipads.wtune.sqlparser.schema;

import sjtu.ipads.wtune.sqlparser.schema.internal.SchemaImpl;

import java.util.Collection;

public interface Schema {
  Collection<? extends Table> tables();

  String dbType();

  Table table(String name);

  void patch(Iterable<SchemaPatch> patches);

  StringBuilder toDdl(String dbType, StringBuilder buffer);

  static Schema parse(String dbType, String str) {
    return SchemaImpl.build(dbType, str);
  }
}
