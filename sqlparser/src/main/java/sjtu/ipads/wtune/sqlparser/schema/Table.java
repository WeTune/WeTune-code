package sjtu.ipads.wtune.sqlparser.schema;

import java.util.Collection;

public interface Table {
  String schema();

  String name();

  String engine();

  Column column(String name);

  Collection<Column> columns();

  Collection<Constraint> constraints();
}
