package sjtu.ipads.wtune.sql.schema;

import sjtu.ipads.wtune.sql.ast.constants.ConstraintKind;
import sjtu.ipads.wtune.sql.ast.constants.KeyDirection;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.IterableSupport.lazyFilter;
import static sjtu.ipads.wtune.sql.ast.constants.ConstraintKind.*;

public interface Constraint {
  List<Column> columns();

  List<KeyDirection> directions();

  ConstraintKind kind();

  Table refTable();

  List<Column> refColumns();

  StringBuilder toDdl(String dbType, StringBuilder buffer);

  default boolean isIndex() {
    return kind() != NOT_NULL && kind() != CHECK;
  }

  default boolean isUnique() {
    return kind() == PRIMARY || kind() == UNIQUE;
  }

  static Iterable<Constraint> filterUniqueKey(Iterable<Constraint> constraints) {
    return lazyFilter(constraints, Constraint::isUnique);
  }
}