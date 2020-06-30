package sjtu.ipads.wtune.stmt.schema;

import sjtu.ipads.wtune.sqlparser.SQLNode;

import java.lang.System.Logger;
import java.util.*;

import static sjtu.ipads.wtune.sqlparser.SQLNode.ConstraintType.FOREIGN;
import static sjtu.ipads.wtune.sqlparser.SQLNode.TABLE_NAME_TABLE;
import static sjtu.ipads.wtune.sqlparser.SQLNode.Type.TABLE_NAME;
import static sjtu.ipads.wtune.stmt.utils.StmtHelper.simpleName;

public class Schema {
  private static Logger LOG = System.getLogger(Schema.class.getSimpleName());

  private final Map<String, Table> tables = new HashMap<>();

  public Schema addDefinition(SQLNode node) {
    if (node == null) return this;
    if (node.type() == SQLNode.Type.CREATE_TABLE) {
      final Table table = new Table().fromCreateTable(node);
      final String tableName = table.tableName();

      if (tables.containsKey(tableName))
        LOG.log(Logger.Level.INFO, "replace table definition: {0}", tableName);

      tables.put(tableName, table);
    }

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
