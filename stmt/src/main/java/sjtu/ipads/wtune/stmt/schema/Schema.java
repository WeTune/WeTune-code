package sjtu.ipads.wtune.stmt.schema;

import sjtu.ipads.wtune.sqlparser.SQLNode;

import java.lang.System.Logger;
import java.nio.file.Path;
import java.util.*;

import static sjtu.ipads.wtune.sqlparser.SQLNode.*;
import static sjtu.ipads.wtune.sqlparser.SQLNode.ConstraintType.FOREIGN;
import static sjtu.ipads.wtune.sqlparser.SQLNode.Type.*;
import static sjtu.ipads.wtune.stmt.schema.Column.COLUMN_AUTOINCREMENT;
import static sjtu.ipads.wtune.stmt.utils.StmtHelper.simpleName;

public class Schema {
  private static final Logger LOG = System.getLogger(Schema.class.getSimpleName());
  private Path sourcePath;

  private final Map<String, Table> tables = new HashMap<>();

  public Path sourcePath() {
    return sourcePath;
  }

  public void setSourcePath(Path sourcePath) {
    this.sourcePath = sourcePath;
  }

  public Schema addDefinition(SQLNode node) {
    if (node == null) return this;

    final SQLNode.Type type = node.type();
    if (type == CREATE_TABLE) addCreateTable(node);
    else if (type == ALTER_SEQUENCE) addAlterSequence(node);
    else if (type == ALTER_TABLE) addAlterTable(node);
    else if (type == INDEX_DEF) addIndexDef(node);

    return this;
  }

  private void addCreateTable(SQLNode node) {
    final Table table = new Table().fromCreateTable(node);
    final String tableName = table.tableName();

    if (tables.containsKey(tableName))
      LOG.log(Logger.Level.INFO, "replace table definition: {0}", tableName);

    tables.put(tableName, table);
  }

  private void addAlterSequence(SQLNode node) {
    final String operation = node.get(ALTER_SEQUENCE_OPERATION);
    final Object payload = node.get(ALTER_SEQUENCE_PAYLOAD);
    if ("owned_by".equals(operation)) {
      final SQLNode columnName = (SQLNode) payload;

      final Table table = getTable(columnName.get(COLUMN_NAME_TABLE));
      if (table == null) return;

      final Column column = table.getColumn(columnName.get(COLUMN_NAME_COLUMN));
      if (column == null) return;

      column.flag(COLUMN_AUTOINCREMENT);
    }
  }

  private void addAlterTable(SQLNode node) {
    final String tableName = node.get(ALTER_TABLE_NAME).get(TABLE_NAME_TABLE);
    final Table table = getTable(tableName);
    if (table == null) return;

    new TableBuilder(table).fromAlterTable(node);
  }

  private void addIndexDef(SQLNode node) {
    final String tableName = node.get(INDEX_DEF_TABLE).get(TABLE_NAME_TABLE);
    final Table table = getTable(tableName);
    if (table == null) return;

    new TableBuilder(table).fromCreateIndex(node);
  }

  public Schema addPatch(SchemaPatch patch) {
    final Table table = getTable(patch.tableName());
    if (table != null) patch.patch(table);
    return this;
  }

  public Schema buildRefs() {
    for (Table table : tables.values())
      for (Constraint constraint : table.constraints())
        if (constraint.type() == FOREIGN) buildRef(constraint);
    return this;
  }

  public Table getTable(String name) {
    return tables.get(simpleName(name));
  }

  public Collection<Table> tables() {
    return tables.values();
  }

  private void buildRef(Constraint constraint) {
    final SQLNode refTableName = constraint.refTableName();
    final List<SQLNode> refColNames = constraint.refColNames();

    assert constraint.type() == FOREIGN;
    assert refTableName != null;
    assert refColNames != null;

    constraint.setRefTableName(null);
    constraint.setRefColNames(null);

    final Table table = getTable(refTableName);
    if (table == null) {
      LOG.log(Logger.Level.INFO, "unknown ref table: {0}", refTableName);
      return;
    }

    final List<Column> columns = new ArrayList<>(refColNames.size());
    for (SQLNode colName : refColNames) {
      final Column column = table.getColumn(colName);
      if (column == null) {
        LOG.log(
            Logger.Level.INFO, "unknown ref column {1} in table {0}", colName, table.tableName());
        return;
      }
      columns.add(column);
    }

    constraint.setRefTable(table);
    constraint.setRefColumns(columns);
  }

  private Table getTable(SQLNode tableName) {
    assert tableName.type() == TABLE_NAME;
    return getTable(tableName.get(TABLE_NAME_TABLE));
  }
}
