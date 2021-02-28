package sjtu.ipads.wtune.sqlparser.schema;

import sjtu.ipads.wtune.sqlparser.schema.internal.SchemaPatchImpl;

import java.util.List;

public interface SchemaPatch {
  enum Type {
    INDEX,
    BOOLEAN,
    ENUM,
    UNIQUE,
    FOREIGN_KEY
  }

  Type type();

  String schema();

  String table();

  List<String> columns();

  static SchemaPatch build(Type type, String schema, String table, List<String> columns) {
    return new SchemaPatchImpl(type, schema, table, columns);
  }
}
