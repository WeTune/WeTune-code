package sjtu.ipads.wtune.sqlparser.schema;

import static sjtu.ipads.wtune.common.utils.FuncUtils.lazyFilter;

import java.util.Collection;
import sjtu.ipads.wtune.sqlparser.ast.constants.ConstraintType;

public interface Table {
  String schema();

  String name();

  String engine();

  Column column(String name);

  Collection<Column> columns();

  Collection<Constraint> constraints();

  default Iterable<Constraint> constraints(ConstraintType type) {
    return lazyFilter(constraints(), it -> it.type() == type);
  }
}
