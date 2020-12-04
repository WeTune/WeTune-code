package sjtu.ipads.wtune.solver.schema.impl;

import sjtu.ipads.wtune.solver.schema.Column;
import sjtu.ipads.wtune.solver.schema.DataType;
import sjtu.ipads.wtune.solver.schema.Schema;
import sjtu.ipads.wtune.solver.schema.Table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

public class TableImpl implements Table {
  private Schema schema;
  private final List<Column> columns;
  private final String name;

  private TableImpl(List<? extends Column> columns, String name) {
    this.columns = Collections.unmodifiableList(columns);
    this.name = name;
  }

  public static TableImpl create(String name, List<? extends Column> columns) {
    return new TableImpl(columns, name);
  }

  public static Builder builder() {
    return new BuilderImpl();
  }

  void setSchema(Schema schema) {
    this.schema = schema;
  }

  @Override
  public Schema schema() {
    return schema;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public Column column(String name) {
    for (Column column : columns) if (column.name().equals(name)) return column;
    return null;
  }

  @Override
  public List<Column> columns() {
    return columns;
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TableImpl table = (TableImpl) o;
    return Objects.equals(name, table.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  private static class BuilderImpl implements Builder {
    private final List<ColumnImpl> columns = new ArrayList<>();
    private String name;

    @Override
    public Builder name(String name) {
      this.name = requireNonNull(name);
      return this;
    }

    @Override
    public Builder column(String name, DataType type) {
      columns.add(ColumnImpl.create(columns.size(), requireNonNull(name), type));
      return this;
    }

    @Override
    public Builder column(String name, DataType type, boolean notNull) {
      columns.add(ColumnImpl.create(columns.size(), requireNonNull(name), type, notNull));
      return this;
    }

    @Override
    public Table build() {
      final TableImpl table = TableImpl.create(name, columns);
      columns.forEach(it -> it.setTable(table));
      return table;
    }
  }
}
