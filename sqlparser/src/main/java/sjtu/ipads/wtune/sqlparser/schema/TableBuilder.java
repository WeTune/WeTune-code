package sjtu.ipads.wtune.sqlparser.schema;

import sjtu.ipads.wtune.common.tree.AstNode;
import sjtu.ipads.wtune.sqlparser.ast1.SqlKind;
import sjtu.ipads.wtune.sqlparser.ast1.SqlNode;
import sjtu.ipads.wtune.sqlparser.ast1.SqlNodes;
import sjtu.ipads.wtune.sqlparser.ast1.constants.ConstraintKind;
import sjtu.ipads.wtune.sqlparser.ast1.constants.KeyDirection;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.common.utils.Commons.coalesce;
import static sjtu.ipads.wtune.sqlparser.ast1.SqlNodeFields.*;
import static sjtu.ipads.wtune.sqlparser.ast1.constants.ConstraintKind.FOREIGN;
import static sjtu.ipads.wtune.sqlparser.ast1.constants.KeyDirection.ASC;

class TableBuilder {
  private final TableImpl table;

  private TableBuilder(TableImpl table) {
    this.table = table;
  }

  static TableBuilder fromCreateTable(SqlNode tableDef) {
    final TableBuilder builder = new TableBuilder(TableImpl.build(tableDef));

    tableDef.$(CreateTable_Cols).forEach(it -> builder.setColumn((SqlNode) it));
    tableDef.$(CreateTable_Cons).forEach(it -> builder.setConstraint((SqlNode) it));

    return builder;
  }

  TableBuilder fromAlterTable(SqlNode alterTable) {
    for (AstNode<SqlKind> action : alterTable.$(AlterTable_Actions))
      switch (action.$(AlterTableAction_Name)) {
        case "add_constraint" -> setConstraint((SqlNode) action.$(AlterTableAction_Payload));
        case "modify_column" -> setColumn((SqlNode) action.get(AlterTableAction_Payload));
      }
    return this;
  }

  TableBuilder fromCreateIndex(SqlNode createIndex) {
    setConstraint(createIndex);
    return this;
  }

  TableImpl table() {
    return table;
  }

  private void setColumn(SqlNode colDef) {
    final ColumnImpl column = ColumnImpl.build(table.name(), colDef);
    table.addColumn(column);

    final EnumSet<ConstraintKind> constraints = colDef.$(ColDef_Cons);
    if (constraints != null)
      for (ConstraintKind cType : constraints) {
        final ConstraintImpl c = ConstraintImpl.build(cType, singletonList(column));

        table.addConstraint(c);
        column.addConstraint(c);
      }

    final SqlNode references = colDef.$(ColDef_Ref);
    if (references != null) {
      final ConstraintImpl c = ConstraintImpl.build(FOREIGN, singletonList(column));
      c.setRefTableName(references.$(Reference_Table));
      c.setRefColNames(references.$(Reference_Cols));

      table.addConstraint(c);
      column.addConstraint(c);
    }
  }

  private void setConstraint(SqlNode constraintDef) {
    final SqlNodes keys = constraintDef.$(IndexDef_Keys);
    final List<Column> columns = new ArrayList<>(keys.size());
    final List<KeyDirection> directions = new ArrayList<>(keys.size());
    for (AstNode<SqlKind> key : keys) {
      final String columnName = key.$(KeyPart_Col);
      final ColumnImpl column = table.column(columnName);
      if (column == null) return;
      columns.add(column);
      directions.add(coalesce(key.$(KeyPart_Direction), ASC));
    }

    final ConstraintImpl c = ConstraintImpl.build(constraintDef.$(IndexDef_Cons), columns);
    c.setIndexType(constraintDef.$(IndexDef_Kind));
    c.setDirections(directions);

    final SqlNode refs = constraintDef.$(IndexDef_Refs);
    if (refs != null) {
      c.setRefTableName(refs.$(Reference_Table));
      c.setRefColNames(refs.$(Reference_Cols));
    }

    columns.forEach(col -> ((ColumnImpl) col).addConstraint(c));
    table.addConstraint(c);
  }
}
