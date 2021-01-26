package sjtu.ipads.wtune.sqlparser.rel.internal;

import sjtu.ipads.wtune.sqlparser.SQLParser;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.NodeType;
import sjtu.ipads.wtune.sqlparser.rel.Schema;
import sjtu.ipads.wtune.sqlparser.rel.Table;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.SQLParser.splitSql;
import static sjtu.ipads.wtune.sqlparser.ast.NodeAttr.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.*;
import static sjtu.ipads.wtune.sqlparser.rel.Column.Flag.AUTO_INCREMENT;

public class SchemaImpl implements Schema {
  private final Map<String, TableImpl> tables;

  private SchemaImpl() {
    tables = new HashMap<>();
  }

  public static Schema build(Iterable<SQLNode> defs) {
    final Map<String, TableBuilder> builders = new HashMap<>();
    for (SQLNode def : defs) {
      if (def == null) continue; // skip comment
      final NodeType type = def.nodeType();
      if (type == CREATE_TABLE) addCreateTable(def, builders);
      else if (type == ALTER_SEQUENCE) addAlterSequence(def, builders);
      else if (type == ALTER_TABLE) addAlterTable(def, builders);
      else if (type == INDEX_DEF) addIndexDef(def, builders);
    }
    final SchemaImpl schema = new SchemaImpl();
    builders.values().forEach(it -> schema.addTable(it.table()));
    schema.buildRef();
    return schema;
  }

  public static Schema build(String dbType, String str) {
    return build(listMap(SQLParser.ofDb(dbType)::parseRaw, splitSql(str)));
  }

  private static void addCreateTable(SQLNode node, Map<String, TableBuilder> builders) {
    final TableBuilder builder = TableBuilder.fromCreateTable(node);
    builders.put(builder.table().name(), builder);
  }

  private static void addAlterSequence(SQLNode node, Map<String, TableBuilder> builders) {
    final String operation = node.get(ALTER_SEQUENCE_OPERATION);
    final Object payload = node.get(ALTER_SEQUENCE_PAYLOAD);
    if ("owned_by".equals(operation)) {
      final SQLNode columnName = (SQLNode) payload;

      final TableBuilder builder = builders.get(columnName.get(COLUMN_NAME_TABLE));
      if (builder == null) return;

      final ColumnImpl column = builder.table().column(columnName.get(COLUMN_NAME_COLUMN));
      if (column == null) return;

      column.flag(AUTO_INCREMENT);
    }
  }

  private static void addAlterTable(SQLNode node, Map<String, TableBuilder> builders) {
    final TableBuilder builder = builders.get(node.get(ALTER_TABLE_NAME).get(TABLE_NAME_TABLE));
    if (builder != null) builder.fromAlterTable(node);
  }

  private static void addIndexDef(SQLNode node, Map<String, TableBuilder> builders) {
    final TableBuilder builder = builders.get(node.get(INDEX_DEF_TABLE).get(TABLE_NAME_TABLE));
    if (builder != null) builder.fromCreateIndex(node);
  }

  private void buildRef() {
    for (TableImpl table : tables.values())
      if (table.constraints() != null)
        for (ConstraintImpl constraint : table.constraints()) {
          final SQLNode refTableName = constraint.refTableName();
          if (refTableName != null) {
            final Table ref = table(refTableName.get(TABLE_NAME_TABLE));
            if (ref == null) continue;
            constraint.setRefTable(ref);
            constraint.setRefColumns(
                listMap(it -> ref.column(it.get(COLUMN_NAME_COLUMN)), constraint.refColNames()));
          }
        }
  }

  @Override
  public Collection<? extends Table> tables() {
    return tables.values();
  }

  @Override
  public Table table(String name) {
    return tables.get(name);
  }

  private void addTable(TableImpl table) {
    tables.put(table.name(), table);
  }
}
