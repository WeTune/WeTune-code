package sjtu.ipads.wtune.sqlparser.schema;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.SQLDataType;
import sjtu.ipads.wtune.sqlparser.ast.constants.ConstraintType;
import sjtu.ipads.wtune.sqlparser.schema.internal.ColumnImpl;

import java.util.Collection;
import java.util.List;

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

  String tableName();

  String name();

  String rawDataType();

  SQLDataType dataType();

  Collection<SchemaPatch> patches();

  boolean isFlag(Flag flag);

  Collection<Constraint> constraints();

  default Collection<Constraint> constraints(ConstraintType type) {
    return listFilter(constraints(), it -> it.type() == type);
  }

  default boolean references(List<Column> referred) {
    if (!isFlag(Flag.FOREIGN_KEY)) return false;

    final Collection<Constraint> fks = constraints(ConstraintType.FOREIGN);
    return fks.isEmpty() // not native FK, then it must be patched
        || fks.stream().map(Constraint::refColumns).anyMatch(referred::equals);
  }

  static Column make(String table, ASTNode colDef) {
    return ColumnImpl.build(table, colDef);
  }
}
