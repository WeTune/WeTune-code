package sjtu.ipads.wtune.solver.schema;

import sjtu.ipads.wtune.solver.schema.impl.TableImpl;

import java.util.List;

public interface Table {
  Schema schema();

  String name();

  Column column(String name);

  List<Column> columns();

  static Table create(String name, List<Column> columns) {
    return TableImpl.create(name, columns);
  }

  static Builder builder() {
    return TableImpl.builder();
  }

  interface Builder {
    Builder name(String name);

    Builder column(String name, DataType type);

    Builder column(String name, DataType type, boolean notNull);

    Table build();
  }
}
