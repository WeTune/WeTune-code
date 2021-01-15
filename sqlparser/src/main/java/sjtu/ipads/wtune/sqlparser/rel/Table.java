package sjtu.ipads.wtune.sqlparser.rel;

import java.util.Collection;

public interface Table {
  String schema();

  String name();

  String engine();

  Column column(String name);

  Collection<? extends Column> columns();

  Collection<Constraint> constraints();
}
