package sjtu.ipads.wtune.sql.schema;

import sjtu.ipads.wtune.common.utils.ListSupport;
import sjtu.ipads.wtune.sql.ast1.SqlKind;
import sjtu.ipads.wtune.sql.ast1.SqlNode;

import java.util.*;
import java.util.function.Function;

import static sjtu.ipads.wtune.sql.ast1.SqlKind.*;
import static sjtu.ipads.wtune.sql.ast1.SqlNodeFields.*;
import static sjtu.ipads.wtune.sql.schema.Column.Flag.AUTO_INCREMENT;
import static sjtu.ipads.wtune.sql.util.ASTHelper.simpleName;

class SchemaImpl implements Schema {
  private final String dbType;
  private final Map<String, TableImpl> tables;

  private SchemaImpl(String dbType) {
    this.dbType = dbType;
    this.tables = new HashMap<>();
  }

  static Schema build(String dbType, Iterable<SqlNode> defs) {
    final Map<String, TableBuilder> builders = new HashMap<>();
    for (SqlNode def : defs) {
      if (def == null) continue; // skip comment
      final SqlKind type = def.kind();
      if (type == CreateTable) addCreateTable(def, builders);
      else if (type == AlterSeq) addAlterSequence(def, builders);
      else if (type == AlterTable) addAlterTable(def, builders);
      else if (type == IndexDef) addIndexDef(def, builders);
    }
    final SchemaImpl schema = new SchemaImpl(dbType);
    builders.values().forEach(it -> schema.addTable(it.table()));
    schema.buildRef();
    return schema;
  }

  private static void addCreateTable(SqlNode node, Map<String, TableBuilder> builders) {
    final TableBuilder builder = TableBuilder.fromCreateTable(node);
    builders.put(builder.table().name(), builder);
  }

  private static void addAlterSequence(SqlNode node, Map<String, TableBuilder> builders) {
    final String operation = node.$(AlterSeq_Op);
    final Object payload = node.$(AlterSeq_Payload);
    if ("owned_by".equals(operation)) {
      final SqlNode colName = (SqlNode) payload;

      final TableBuilder builder = builders.get(colName.$(ColName_Table));
      if (builder == null) return;

      final ColumnImpl column = builder.table().column(colName.$(ColName_Col));
      if (column == null) return;

      column.flag(AUTO_INCREMENT);
    }
  }

  private static void addAlterTable(SqlNode node, Map<String, TableBuilder> builders) {
    final TableBuilder builder = builders.get(node.$(AlterTable_Name).$(TableName_Table));
    if (builder != null) builder.fromAlterTable(node);
  }

  private static void addIndexDef(SqlNode node, Map<String, TableBuilder> builders) {
    final TableBuilder builder = builders.get(node.$(IndexDef_Table).$(TableName_Table));
    if (builder != null) builder.fromCreateIndex(node);
  }

  private void buildRef() {
    for (TableImpl table : tables.values())
      if (table.constraints() != null)
        for (Constraint constraint0 : table.constraints()) {
          final ConstraintImpl constraint = (ConstraintImpl) constraint0;
          final SqlNode refTableName = constraint.refTableName();
          if (refTableName != null) {
            final Table ref = table(refTableName.$(TableName_Table));
            if (ref == null) continue;
            constraint.setRefTable(ref);
            constraint.setRefColumns(
                    ListSupport.map((Iterable<SqlNode>) constraint.refColNames(), (Function<? super SqlNode, ? extends Column>) it -> ref.column(it.$(ColName_Col))));
          }
        }
  }

  @Override
  public String dbType() {
    return dbType;
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
    buildRef();
  }

  private void addTable(TableImpl table) {
    tables.put(table.name(), table);
  }

  @Override
  public StringBuilder toDdl(String dbType, StringBuilder buffer) {
    for (TableImpl value : tables.values()) {
      value.toDdl(dbType, buffer);
      buffer.append('\n');
    }
    final Set<Constraint> done = new HashSet<>();
    for (TableImpl value : tables.values()) {
      for (Constraint constraint : value.constraints()) {
        if (done.contains(constraint)) continue;
        done.add(constraint);
        constraint.toDdl(dbType, buffer);
      }
    }
    return buffer;
  }
}
