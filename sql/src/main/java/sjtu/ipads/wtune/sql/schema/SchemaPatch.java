package sjtu.ipads.wtune.sql.schema;

import org.apache.commons.lang3.NotImplementedException;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.Commons.joining;
import static sjtu.ipads.wtune.sql.ast.SqlNode.PostgreSQL;

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

  default String toDDL(String dbType) {
    switch (type()) {
      case FOREIGN_KEY:
        if (PostgreSQL.equals(dbType)) {
          return "ALTER TABLE \"%s\" ADD CONSTRAINT \"fk_%s_%s_refs_%s\" FOREIGN KEY (%s) REFERENCES \"%s\"(\"%s\")"
              .formatted(
                  table(),
                  table(),
                  joining("_", columns()),
                  reference().replace('.', '_'),
                  joining(",", "\"", "\"", true, columns()),
                  reference().split("\\.")[0],
                  reference().split("\\.")[1]);
        } else {
          return "ALTER TABLE `%s` ADD CONSTRAINT `fk_%s_%s_refs_%s` FOREIGN KEY (%s) REFERENCES `%s`(`%s`)"
              .formatted(
                  table(),
                  table(),
                  joining("_", columns()),
                  reference().replace('.', '_'),
                  joining(",", "`", "`", true, columns()),
                  reference().split("\\.")[0],
                  reference().split("\\.")[1]);
        }
      case INDEX:
      case UNIQUE:
        if (PostgreSQL.equals(dbType)) {
          return "CREATE %sINDEX \"index_%s\" ON \"%s\"(%s)"
              .formatted(
                  type() == Type.UNIQUE ? "UNIQUE " : "",
                  joining("_", columns()),
                  table(),
                  joining(",", "\"", "\"", true, columns()));
        } else {
          return "CREATE %sINDEX `index_%s` ON `%s`(%s)"
              .formatted(
                  type() == Type.UNIQUE ? "UNIQUE " : "",
                  joining("_", columns()),
                  table(),
                  joining(",", "`", "`", true, columns()));
        }

      default:
        throw new NotImplementedException();
    }
  }

  static SchemaPatch build(
      Type type, String schema, String table, List<String> columns, String reference) {
    return new SchemaPatchImpl(type, schema, table, columns, reference);
  }
}
