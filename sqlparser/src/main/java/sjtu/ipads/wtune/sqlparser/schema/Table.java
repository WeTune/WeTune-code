package sjtu.ipads.wtune.sqlparser.schema;

import sjtu.ipads.wtune.sqlparser.ast.constants.ConstraintType;

import java.util.Collection;
import java.util.EnumSet;

import static sjtu.ipads.wtune.common.utils.FuncUtils.lazyFilter;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ConstraintType.*;

public interface Table {
  String schema();

  String name();

  String engine();

  Column column(String name);

  Collection<Column> columns();

  Collection<Constraint> constraints();

  StringBuilder toDdl(String dbType, StringBuilder buffer);

  default Iterable<Constraint> constraints(ConstraintType type) {
    if (type == UNIQUE)
      return lazyFilter(constraints(), it -> UNIQUE_CONSTRAINT.contains(it.type()));
    else return lazyFilter(constraints(), it -> it.type() == type);
  }

  EnumSet<ConstraintType> INDEXED_CONSTRAINT = EnumSet.of(UNIQUE, PRIMARY, FOREIGN);
  EnumSet<ConstraintType> UNIQUE_CONSTRAINT = EnumSet.of(UNIQUE, PRIMARY);
}
