package sjtu.ipads.wtune.sqlparser.schema;

import sjtu.ipads.wtune.sqlparser.ast1.constants.ConstraintKind;
import sjtu.ipads.wtune.sqlparser.ast1.constants.KeyDirection;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.IterableSupport.lazyFilter;
import static sjtu.ipads.wtune.sqlparser.ast1.constants.ConstraintKind.*;

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
