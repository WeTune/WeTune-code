package sjtu.ipads.wtune.sqlparser.rel;

import sjtu.ipads.wtune.sqlparser.ast.SQLDataType;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.ConstraintType;
import sjtu.ipads.wtune.sqlparser.rel.internal.ColumnImpl;

import java.util.Collection;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listFilter;

public interface Column {
  enum Flag {
    UNIQUE,
    INDEXED,
    FOREIGN_KEY,
    PRIMARY,
    NOT_NULL,
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

  default Collection<Constraint> constraints(ConstraintType type) {
    return listFilter(it -> it.type() == type, constraints());
  }

  static Column make(String table, SQLNode colDef) {
    return ColumnImpl.build(table, colDef);
  }
}
