package sjtu.ipads.wtune.sqlparser.rel;

import sjtu.ipads.wtune.sqlparser.ast.SQLDataType;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.rel.internal.ColumnImpl;

import java.util.Collection;

public interface Column {
  enum Flag {
    UNIQUE,
    INDEXED,
    FOREIGN_KEY,
    PRIMARY,
    GENERATED,
    HAS_DEFAULT,
    HAS_CHECK,
    AUTO_INCREMENT
  }

  String table();

  String name();

  String rawDataType();

  SQLDataType dataType();

  boolean isFlagged(Flag flag);

  Collection<Constraint> constraints();

  static Column make(String table, SQLNode colDef) {
    return ColumnImpl.build(table, colDef);
  }
}
