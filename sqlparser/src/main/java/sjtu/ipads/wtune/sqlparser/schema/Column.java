package sjtu.ipads.wtune.sqlparser.schema;

import sjtu.ipads.wtune.sqlparser.ast.SQLDataType;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.ConstraintType;
import sjtu.ipads.wtune.sqlparser.schema.internal.ColumnImpl;

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
    AUTO_INCREMENT,
    IS_BOOLEAN,
    IS_ENUM
  }

  String table();

  String name();

  String rawDataType();

  SQLDataType dataType();

  Collection<SchemaPatch> patches();

  boolean isFlag(Flag flag);

  Collection<Constraint> constraints();

  default Collection<Constraint> constraints(ConstraintType type) {
    return listFilter(it -> it.type() == type, constraints());
  }

  static Column make(String table, SQLNode colDef) {
    return ColumnImpl.build(table, colDef);
  }
}
