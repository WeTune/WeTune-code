package sjtu.ipads.wtune.solver.schema;

import sjtu.ipads.wtune.solver.schema.impl.ColumnImpl;

public interface Column {
  Table table();

  String name();

  int index();

  DataType dataType();

  boolean notNull();

  static Column create(int index, String name, DataType dataType) {
    return ColumnImpl.create(index, name, dataType);
  }
}
