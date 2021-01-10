package sjtu.ipads.wtune.stmt.schema;

import com.google.common.collect.Iterables;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;

import java.util.*;

import static sjtu.ipads.wtune.sqlparser.ast.NodeAttrs.COLUMN_NAME_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.COLUMN_NAME;
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

  private final Map<String, Column> columns = new HashMap<>();
  private final Set<Constraint> constraints = new HashSet<>();

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

  public Iterable<Constraint> indexes() {
    return Iterables.filter(constraints, Constraint::isIndex);
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

  @Override
  public String toString() {
    return schemaName == null ? tableName : (schemaName + "." + tableName);
  }

  Column getColumn(SQLNode columnName) {
    assert columnName.nodeType() == COLUMN_NAME;
    return getColumn(columnName.get(COLUMN_NAME_COLUMN));
  }
}
