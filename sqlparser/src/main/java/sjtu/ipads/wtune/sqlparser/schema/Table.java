package sjtu.ipads.wtune.sqlparser.schema;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listFilter;

import java.util.Collection;
import sjtu.ipads.wtune.sqlparser.ast.constants.ConstraintType;

public interface Table {
  String schema();

  String name();

  String engine();

  Column column(String name);

  Collection<Column> columns();

  Collection<Constraint> constraints();

  default Collection<Constraint> constraints(ConstraintType type) {
    return listFilter(constraints(), it -> it.type() == type);
  }
}
