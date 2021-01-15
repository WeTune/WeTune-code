package sjtu.ipads.wtune.stmt.schema;

import sjtu.ipads.wtune.common.utils.Commons;
import sjtu.ipads.wtune.sqlparser.ast.NodeAttrs;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.ConstraintType;
import sjtu.ipads.wtune.sqlparser.ast.constants.KeyDirection;

import java.util.*;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.WARNING;
import static java.util.Collections.singleton;
import static java.util.Objects.requireNonNull;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.CREATE_TABLE;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_COLUMN;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_TABLE;

class TableBuilder {
  private static final System.Logger LOG = System.getLogger("Stmt.Core.Table");

  private final Table table;

  TableBuilder(Table table) {
    this.table = requireNonNull(table);
  }

  Table fromCreateTable(SQLNode createTable) {
    if (!CREATE_TABLE.isInstance(createTable)) {
      LOG.log(DEBUG, "illegal statement: " + createTable);
      return table;
    }

    createTable.put(RESOLVED_TABLE, table);

    if (SQLNode.POSTGRESQL.equals(createTable.dbType())) {
      table.setEngine(SQLNode.POSTGRESQL);
    } else {
      table.setEngine(Commons.coalesce(createTable.get(NodeAttrs.CREATE_TABLE_ENGINE), "innodb"));
    }

    setName(createTable.get(NodeAttrs.CREATE_TABLE_NAME));

    createTable.get(NodeAttrs.CREATE_TABLE_COLUMNS).forEach(this::setColumn);
    createTable.get(NodeAttrs.CREATE_TABLE_COLUMNS).forEach(this::setConstraintFromColumnDef);
    createTable.get(NodeAttrs.CREATE_TABLE_CONSTRAINTS).forEach(this::setConstraint);

    return table;
  }

  Table fromAlterTable(SQLNode alterTable) {
    final List<SQLNode> actions = alterTable.get(NodeAttrs.ALTER_TABLE_ACTIONS);
    for (SQLNode action : actions) {
      final String actionName = action.get(NodeAttrs.ALTER_TABLE_ACTION_NAME);
      final Object payload = action.get(NodeAttrs.ALTER_TABLE_ACTION_PAYLOAD);
      if ("add_constraint".equals(actionName)) {
        final SQLNode constraint = (SQLNode) payload;
        setConstraint(constraint);
      }
    }
    return table;
  }

  Table fromCreateIndex(SQLNode createIndex) {
    setConstraint(createIndex);
    return table;
  }

  private void setName(SQLNode name) {
    table.setSchemaName(name.get(NodeAttrs.TABLE_NAME_SCHEMA));
    table.setTableName(name.get(NodeAttrs.TABLE_NAME_TABLE));
  }

  private void setColumn(SQLNode column) {
    table.addColumn(ColumnBuilder.fromColumnDef(column));
  }

  private void setConstraintFromColumnDef(SQLNode colDef) {
    final Column column = colDef.get(RESOLVED_COLUMN);
    final EnumSet<ConstraintType> constraints = colDef.get(NodeAttrs.COLUMN_DEF_CONS);
    if (constraints == null) return;

    for (ConstraintType cType : constraints) {
      final Constraint c = new Constraint();
      c.setType(cType);
      c.setColumns(singleton(column));

      table.addConstraint(c);
      column.addConstraint(c);
    }

    final SQLNode references = colDef.get(NodeAttrs.COLUMN_DEF_REF);
    if (references != null) {
      final Constraint c = new Constraint();
      c.setType(ConstraintType.FOREIGN);
      c.setColumns(singleton(column));
      c.setRefTableName(references.get(NodeAttrs.REFERENCES_TABLE));
      c.setRefColNames(references.get(NodeAttrs.REFERENCES_COLUMNS));

      column.addConstraint(c);
      table.addConstraint(c);
    }
  }

  private void setConstraint(SQLNode constraintDef) {
    final Constraint c = new Constraint();
    c.setType(constraintDef.get(NodeAttrs.INDEX_DEF_CONS));
    c.setIndexType(constraintDef.get(NodeAttrs.INDEX_DEF_TYPE));

    final List<SQLNode> keys = constraintDef.get(NodeAttrs.INDEX_DEF_KEYS);
    final Set<Column> columns = new LinkedHashSet<>(keys.size());
    final List<KeyDirection> directions = new ArrayList<>(keys.size());

    for (SQLNode key : keys) {
      final String columnName = key.get(NodeAttrs.KEY_PART_COLUMN);
      if (columnName == null) {
        LOG.log(DEBUG, "expr-based index: {0} in {1}", key, table.tableName());
        continue;
      }
      final Column column = table.getColumn(columnName);
      if (column == null) {
        LOG.log(
            WARNING,
            "invalid column {0} in constraint definition: {1}",
            columnName,
            constraintDef.toString());
        return;
      }

      columns.add(column);
      directions.add(Commons.coalesce(key.get(NodeAttrs.KEY_PART_DIRECTION), KeyDirection.ASC));
    }

    if (columns.isEmpty()) return;

    final SQLNode refs = constraintDef.get(NodeAttrs.INDEX_DEF_REFS);
    if (refs != null) {
      c.setRefTableName(refs.get(NodeAttrs.REFERENCES_TABLE));
      c.setRefColNames(refs.get(NodeAttrs.REFERENCES_COLUMNS));
    }

    c.setColumns(columns);
    c.setDirections(directions);

    columns.forEach(col -> col.addConstraint(c));
    table.addConstraint(c);
  }
}
