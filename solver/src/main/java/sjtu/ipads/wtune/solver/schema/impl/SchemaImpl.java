package sjtu.ipads.wtune.solver.schema.impl;

import sjtu.ipads.wtune.solver.schema.Column;
import sjtu.ipads.wtune.solver.schema.Schema;
import sjtu.ipads.wtune.solver.schema.Table;

import java.util.*;
import java.util.function.Function;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

public class SchemaImpl implements Schema {
  private final Map<String, Table> tables;
  private final Map<List<Column>, List<Column>> foreignKeys;

  private SchemaImpl(Map<String, Table> tables, Map<List<Column>, List<Column>> foreignKeys) {
    this.tables = tables;
    this.foreignKeys = foreignKeys;
  }

  public static SchemaImpl create(
      Map<String, Table> tables, Map<List<Column>, List<Column>> foreignKeys) {
    return new SchemaImpl(tables, foreignKeys);
  }

  public static Builder builder() {
    return new BuilderImpl();
  }

  @Override
  public Table table(String name) {
    return tables.get(name);
  }

  @Override
  public Collection<Table> tables() {
    return tables.values();
  }

  @Override
  public Map<List<Column>, List<Column>> foreignKeys() {
    return foreignKeys;
  }

  private static class BuilderImpl implements Builder {
    private final Map<String, Table> tables = new HashMap<>();
    private final Map<List<Column>, List<Column>> foreignKeys = new HashMap<>();

    @Override
    public Builder table(Function<Table.Builder, Table.Builder> builder) {
      final Table table = builder.apply(Table.builder()).build();
      tables.put(table.name(), table);
      return this;
    }

    @Override
    public Builder foreignKey(List<String> referee, List<String> referred) {
      if (referee.size() != referred.size())
        throw new IllegalArgumentException("# of columns in both side of FK must be equal");

      final List<Column> referees = listMap(this::getColumn, referee);
      final List<Column> referreds = listMap(this::getColumn, referred);
      foreignKeys.put(referees, referreds);
      return this;
    }

    private Column getColumn(String qualifiedName) {
      final int dotIndex = qualifiedName.indexOf('.');
      if (dotIndex == -1)
        throw new IllegalArgumentException("column name in foreign key should be qualified");
      final String tableName = qualifiedName.substring(0, dotIndex);
      final String columnName = qualifiedName.substring(dotIndex + 1);
      final Table table = tables.get(tableName);
      if (table != null) {
        final Column column = table.column(columnName);
        if (column != null) return column;
      }
      throw new NoSuchElementException("no such column: " + qualifiedName);
    }

    @Override
    public Schema build() {
      final SchemaImpl schema = SchemaImpl.create(tables, foreignKeys);
      tables.values().forEach(it -> ((TableImpl) it).setSchema(schema));
      return schema;
    }
  }
}
