package sjtu.ipads.wtune.sqlparser.schema;

import java.util.List;
import sjtu.ipads.wtune.sqlparser.schema.internal.SchemaPatchImpl;

public interface SchemaPatch {
  enum Type {
    NOT_NULL,
    INDEX,
    BOOLEAN,
    ENUM,
    UNIQUE,
    FOREIGN_KEY;
  }

  Type type();

  String schema();

  String table();

  List<String> columns();

  String reference();

  static SchemaPatch build(
      Type type, String schema, String table, List<String> columns, String reference) {
    return new SchemaPatchImpl(type, schema, table, columns, reference);
  }
}
