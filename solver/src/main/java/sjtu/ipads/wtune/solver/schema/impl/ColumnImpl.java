package sjtu.ipads.wtune.solver.schema.impl;

import sjtu.ipads.wtune.solver.schema.Column;
import sjtu.ipads.wtune.solver.schema.DataType;
import sjtu.ipads.wtune.solver.schema.Table;

import java.util.Objects;

public class ColumnImpl implements Column {
  private Table table;

  private final int index;
  private final String name;
  private final DataType dataType;
  private final boolean notNull;

  private ColumnImpl(int index, String name, DataType dataType, boolean notNull) {
    this.index = index;
    this.name = name;
    this.dataType = dataType;
    this.notNull = notNull;
  }

  public static ColumnImpl create(int index, String name, DataType dataType, boolean notNull) {
    return new ColumnImpl(index, name, dataType, notNull);
  }

  public static ColumnImpl create(int index, String name, DataType dataType) {
    return new ColumnImpl(index, name, dataType, true);
  }

  void setTable(Table table) {
    this.table = table;
  }

  @Override
  public Table table() {
    return table;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public int index() {
    return index;
  }

  public DataType dataType() {
    return dataType;
  }

  @Override
  public boolean notNull() {
    return notNull;
  }

  @Override
  public String toString() {
    return (table == null ? "?" : table.name()) + "." + name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ColumnImpl column = (ColumnImpl) o;
    return Objects.equals(table, column.table) && Objects.equals(name, column.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(table, name);
  }
}
