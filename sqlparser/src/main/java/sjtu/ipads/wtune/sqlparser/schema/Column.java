package sjtu.ipads.wtune.sqlparser.schema;

import sjtu.ipads.wtune.sqlparser.ast1.SqlDataType;
import sjtu.ipads.wtune.sqlparser.ast1.SqlNode;
import sjtu.ipads.wtune.sqlparser.ast1.constants.ConstraintKind;

import java.util.Collection;
import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listFilter;
import static sjtu.ipads.wtune.sqlparser.ast1.constants.ConstraintKind.FOREIGN;

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

  SqlDataType dataType();

  Collection<SchemaPatch> patches();

  boolean isFlag(Flag flag);

  Collection<Constraint> constraints();

  StringBuilder toDdl(String dbType, StringBuilder buffer);

  default Collection<Constraint> constraints(ConstraintKind type) {
    return listFilter(constraints(), it -> it.kind() == type);
  }

  default boolean references(List<Column> referred) {
    if (!isFlag(Flag.FOREIGN_KEY)) return false;

    final Collection<Constraint> fks = constraints(FOREIGN);
    return fks.isEmpty() // not native FK, then it must be patched
        || fks.stream().map(Constraint::refColumns).anyMatch(referred::equals);
  }

  static Column mk(String table, SqlNode colDef) {
    return ColumnImpl.build(table, colDef);
  }
}
