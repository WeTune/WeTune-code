package sjtu.ipads.wtune.stmt.schema;

import sjtu.ipads.wtune.sqlparser.SQLNode;

import java.util.*;

import static sjtu.ipads.wtune.sqlparser.SQLNode.COLUMN_NAME_COLUMN;
import static sjtu.ipads.wtune.sqlparser.SQLNode.Type.COLUMN_NAME;
import static sjtu.ipads.wtune.stmt.utils.StmtHelper.simpleName;

/**
 * Table.
 *
 * <p>Note: equals and hashCode are override as only comparing the `tableName`.
 */
public class Table {
  private String schemaName;
  private String tableName;
  private String engine;

  private Map<String, Column> columns = new HashMap<>();
  private Set<Constraint> constraints = new HashSet<>();

  public Table fromCreateTable(SQLNode createTable) {
    return new TableBuilder(this).fromCreateTable(createTable);
  }

  public String schemaName() {
    return schemaName;
  }

  public String tableName() {
    return tableName;
  }

  public String engine() {
    return engine;
  }

  public Collection<Column> columns() {
    return columns.values();
  }

  public Set<Constraint> constraints() {
    return constraints;
  }

  public Column getColumn(String name) {
    return columns.get(simpleName(name));
  }

  public void setSchemaName(String schemaName) {
    this.schemaName = simpleName(schemaName);
  }

  public void setTableName(String tableName) {
    this.tableName = simpleName(tableName);
  }

  public void setEngine(String engine) {
    this.engine = engine;
  }

  public void addColumn(Column column) {
    if (column == null) return;
    columns.put(column.columnName(), column);
    column.setTable(this);
  }

  public void addConstraint(Constraint constraint) {
    constraints.add(constraint);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Table table = (Table) o;
    return Objects.equals(tableName, table.tableName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tableName);
  }

  Column getColumn(SQLNode columnName) {
    assert columnName.type() == COLUMN_NAME;
    return columns.get(columnName.get(COLUMN_NAME_COLUMN));
  }
}
