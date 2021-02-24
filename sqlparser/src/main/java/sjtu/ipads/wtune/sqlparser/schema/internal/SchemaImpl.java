package sjtu.ipads.wtune.sqlparser.schema.internal;

import sjtu.ipads.wtune.sqlparser.ASTParser;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.NodeType;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.sqlparser.schema.SchemaPatch;
import sjtu.ipads.wtune.sqlparser.schema.Table;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.ASTParser.splitSql;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.*;
import static sjtu.ipads.wtune.sqlparser.schema.Column.Flag.AUTO_INCREMENT;
import static sjtu.ipads.wtune.sqlparser.util.ASTHelper.simpleName;

public class SchemaImpl implements Schema {
  private final Map<String, TableImpl> tables;

  private SchemaImpl() {
    tables = new HashMap<>();
  }

  public static Schema build(Iterable<ASTNode> defs) {
    final Map<String, TableBuilder> builders = new HashMap<>();
    for (ASTNode def : defs) {
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
    return build(listMap(ASTParser.ofDb(dbType)::parseRaw, splitSql(str)));
  }

  private static void addCreateTable(ASTNode node, Map<String, TableBuilder> builders) {
    final TableBuilder builder = TableBuilder.fromCreateTable(node);
    builders.put(builder.table().name(), builder);
  }

  private static void addAlterSequence(ASTNode node, Map<String, TableBuilder> builders) {
    final String operation = node.get(ALTER_SEQUENCE_OPERATION);
    final Object payload = node.get(ALTER_SEQUENCE_PAYLOAD);
    if ("owned_by".equals(operation)) {
      final ASTNode columnName = (ASTNode) payload;

      final TableBuilder builder = builders.get(columnName.get(COLUMN_NAME_TABLE));
      if (builder == null) return;

      final ColumnImpl column = builder.table().column(columnName.get(COLUMN_NAME_COLUMN));
      if (column == null) return;

      column.flag(AUTO_INCREMENT);
    }
  }

  private static void addAlterTable(ASTNode node, Map<String, TableBuilder> builders) {
    final TableBuilder builder = builders.get(node.get(ALTER_TABLE_NAME).get(TABLE_NAME_TABLE));
    if (builder != null) builder.fromAlterTable(node);
  }

  private static void addIndexDef(ASTNode node, Map<String, TableBuilder> builders) {
    final TableBuilder builder = builders.get(node.get(INDEX_DEF_TABLE).get(TABLE_NAME_TABLE));
    if (builder != null) builder.fromCreateIndex(node);
  }

  private void buildRef() {
    for (TableImpl table : tables.values())
      if (table.constraints() != null)
        for (ConstraintImpl constraint : table.constraints()) {
          final ASTNode refTableName = constraint.refTableName();
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
  public TableImpl table(String name) {
    return tables.get(simpleName(name));
  }

  @Override
  public void patch(Iterable<SchemaPatch> patches) {
    for (SchemaPatch patch : patches) {
      final TableImpl table = table(patch.table());
      if (table != null) table.addPatch(patch);
    }
  }

  private void addTable(TableImpl table) {
    tables.put(table.name(), table);
  }
}
