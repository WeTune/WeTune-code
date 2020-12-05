package sjtu.ipads.wtune.solver.schema;

import sjtu.ipads.wtune.solver.schema.impl.TableImpl;

import java.util.List;
import java.util.Set;

public interface Table {
  Schema schema();

  String name();

  Column column(String name);

  List<Column> columns();

  Set<Set<Column>> uniqueKeys();

  static Builder builder() {
    return TableImpl.builder();
  }

  interface Builder {
    Builder name(String name);

    Builder column(String name, DataType type);

    Builder column(String name, DataType type, boolean notNull);

    Builder uniqueKey(String first, String... others);

    Table build();
  }
}
