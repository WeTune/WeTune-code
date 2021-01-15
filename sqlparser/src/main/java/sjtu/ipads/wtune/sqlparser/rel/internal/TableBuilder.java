package sjtu.ipads.wtune.sqlparser.rel.internal;

import sjtu.ipads.wtune.sqlparser.ast.NodeAttrs;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.ConstraintType;
import sjtu.ipads.wtune.sqlparser.ast.constants.KeyDirection;
import sjtu.ipads.wtune.sqlparser.rel.Constraint;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.common.utils.FuncUtils.coalesce;
import static sjtu.ipads.wtune.sqlparser.ast.NodeAttrs.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ConstraintType.FOREIGN;
import static sjtu.ipads.wtune.sqlparser.ast.constants.KeyDirection.ASC;

class TableBuilder {
  private final TableImpl table;

  private TableBuilder(TableImpl table) {
    this.table = table;
  }

  static TableBuilder fromCreateTable(SQLNode tableDef) {
    final TableBuilder builder = new TableBuilder(TableImpl.build(tableDef));

    tableDef.get(NodeAttrs.CREATE_TABLE_COLUMNS).forEach(builder::setColumn);
    tableDef.get(NodeAttrs.CREATE_TABLE_CONSTRAINTS).forEach(builder::setConstraint);

    return builder;
  }

  TableBuilder fromAlterTable(SQLNode alterTable) {
    for (SQLNode action : alterTable.get(ALTER_TABLE_ACTIONS))
      if ("add_constraint".equals(action.get(ALTER_TABLE_ACTION_NAME)))
        setConstraint((SQLNode) action.get(ALTER_TABLE_ACTION_PAYLOAD));
    return this;
  }

  TableBuilder fromCreateIndex(SQLNode createIndex) {
    setConstraint(createIndex);
    return this;
  }

  TableImpl table() {
    return table;
  }

  private void setColumn(SQLNode colDef) {
    final ColumnImpl column = ColumnImpl.build(table.name(), colDef);
    table.addColumn(column);

    final EnumSet<ConstraintType> constraints = colDef.get(NodeAttrs.COLUMN_DEF_CONS);
    if (constraints == null) return;

    for (ConstraintType cType : constraints) {
      final Constraint c = ConstraintImpl.build(cType, singletonList(column));

      table.addConstraint(c);
      column.addConstraint(c);
    }

    final SQLNode references = colDef.get(NodeAttrs.COLUMN_DEF_REF);
    if (references != null) {
      final ConstraintImpl c = ConstraintImpl.build(FOREIGN, singletonList(column));
      c.setRefTableName(references.get(REFERENCES_TABLE));
      c.setRefColNames(references.get(REFERENCES_COLUMNS));

      table.addConstraint(c);
      column.addConstraint(c);
    }
  }

  private void setConstraint(SQLNode constraintDef) {
    final List<SQLNode> keys = constraintDef.get(INDEX_DEF_KEYS);
    final List<ColumnImpl> columns = new ArrayList<>(keys.size());
    final List<KeyDirection> directions = new ArrayList<>(keys.size());
    for (SQLNode key : keys) {
      final String columnName = key.get(KEY_PART_COLUMN);
      final ColumnImpl column = table.column(columnName);
      columns.add(column);
      directions.add(coalesce(key.get(KEY_PART_DIRECTION), ASC));
    }

    final ConstraintImpl c = ConstraintImpl.build(constraintDef.get(INDEX_DEF_CONS), columns);
    c.setIndexType(constraintDef.get(NodeAttrs.INDEX_DEF_TYPE));
    c.setDirections(directions);

    final SQLNode refs = constraintDef.get(NodeAttrs.INDEX_DEF_REFS);
    if (refs != null) {
      c.setRefTableName(refs.get(REFERENCES_TABLE));
      c.setRefColNames(refs.get(REFERENCES_COLUMNS));
    }

    columns.forEach(col -> col.addConstraint(c));
    table.addConstraint(c);
  }
}
