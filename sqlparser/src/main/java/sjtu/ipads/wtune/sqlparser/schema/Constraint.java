package sjtu.ipads.wtune.sqlparser.schema;

import sjtu.ipads.wtune.sqlparser.ast.constants.ConstraintType;
import sjtu.ipads.wtune.sqlparser.ast.constants.KeyDirection;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.lazyFilter;

public interface Constraint {
  List<Column> columns();

  List<KeyDirection> directions();

  ConstraintType type();

  Table refTable();

  List<Column> refColumns();

  StringBuilder toDdl(String dbType, StringBuilder buffer);

  default boolean isIndex() {
    return type() != ConstraintType.NOT_NULL && type() != ConstraintType.CHECK;
  }

  default boolean isUnique() {
    return type() == ConstraintType.PRIMARY || type() == ConstraintType.UNIQUE;
  }

  static Iterable<Constraint> filterUniqueKey(Iterable<Constraint> constraints) {
    return lazyFilter(constraints, Constraint::isUnique);
  }
}
