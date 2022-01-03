package sjtu.ipads.wtune.sql.schema;

import sjtu.ipads.wtune.common.utils.ListSupport;
import sjtu.ipads.wtune.sql.ast1.SqlContext;
import sjtu.ipads.wtune.sql.ast1.SqlNode;
import sjtu.ipads.wtune.sql.ast1.SqlNodes;
import sjtu.ipads.wtune.sql.schema.SchemaPatch.Type;
import sjtu.ipads.wtune.sql.util.ASTHelper;

import java.util.*;
import java.util.function.Function;

import static sjtu.ipads.wtune.common.utils.Commons.coalesce;
import static sjtu.ipads.wtune.common.utils.Commons.joining;
import static sjtu.ipads.wtune.sql.ast.ASTNode.POSTGRESQL;
import static sjtu.ipads.wtune.sql.ast1.SqlKind.ColName;
import static sjtu.ipads.wtune.sql.ast1.SqlKind.TableName;
import static sjtu.ipads.wtune.sql.ast1.SqlNodeFields.*;
import static sjtu.ipads.wtune.sql.ast1.constants.ConstraintKind.FOREIGN;
import static sjtu.ipads.wtune.sql.ast1.constants.ConstraintKind.UNIQUE;
import static sjtu.ipads.wtune.sql.util.ASTHelper.simpleName;

class TableImpl implements Table {
  private final String schema;
  private final String name;
  private final String engine;
  private final Map<String, Column> columns;
  private List<Constraint> constraints;

  TableImpl(String schema, String name, String engine) {
    this.schema = schema;
    this.name = name;
    this.engine = engine;
    this.columns = new LinkedHashMap<>();
  }

  static TableImpl build(SqlNode tableDef) {
    final SqlNode tableName = tableDef.$(CreateTable_Name);
    final String schema = simpleName(tableName.$(TableName_Schema));
    final String name = simpleName(tableName.$(TableName_Table));
    final String engine =
        POSTGRESQL.equals(tableDef.dbType())
            ? POSTGRESQL
            : coalesce(tableDef.$(CreateTable_Engine), "innodb");

    return new TableImpl(schema, name, engine);
  }

  @Override
  public String schema() {
    return schema;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public String engine() {
    return engine;
  }

  @Override
  public Collection<Column> columns() {
    return columns.values();
  }

  @Override
  public Collection<Constraint> constraints() {
    return constraints == null ? Collections.emptyList() : constraints;
  }

  @Override
  public ColumnImpl column(String name) {
    return (ColumnImpl) columns.get(simpleName(name));
  }

  void addPatch(SchemaPatch patch) {
    for (String name : patch.columns()) {
      final ColumnImpl column = column(name);
      if (column != null) column.addPatch(patch);
    }

    if (patch.type() == SchemaPatch.Type.UNIQUE) {
      final List<Column> columns = ListSupport.map((Iterable<String>) patch.columns(), (Function<? super String, ? extends Column>) this::column);
      final ConstraintImpl constraint = ConstraintImpl.build(UNIQUE, columns);

      addConstraint(constraint);
      columns.forEach(it -> ((ColumnImpl) it).addConstraint(constraint));
    }

    if (patch.type() == Type.FOREIGN_KEY) {
      final List<Column> columns = ListSupport.map((Iterable<String>) patch.columns(), (Function<? super String, ? extends Column>) this::column);
      final ConstraintImpl constraint = ConstraintImpl.build(FOREIGN, columns);

      final String[] split = patch.reference().split("\\.");
      if (split.length != 2) throw new IllegalArgumentException("illegal patch: " + patch);

      final SqlContext ctx = SqlContext.mk(4);
      final SqlNode tableName = SqlNode.mk(ctx, TableName);
      tableName.$(TableName_Table, split[0]);
      constraint.setRefTableName(tableName);

      final SqlNode colName = SqlNode.mk(ctx, ColName);
      colName.$(ColName_Col, split[1]);
      constraint.setRefColNames(SqlNodes.mk(ctx, Collections.singletonList(colName)));

      addConstraint(constraint);
      columns.forEach(it -> ((ColumnImpl) it).addConstraint(constraint));
    }
  }

  void addColumn(Column column) {
    columns.put(column.name(), column);
  }

  void addConstraint(ConstraintImpl constraint) {
    if (constraints == null) constraints = new ArrayList<>();
    constraints.add(constraint);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TableImpl table = (TableImpl) o;
    return Objects.equals(schema, table.schema) && Objects.equals(name, table.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(schema, name);
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public StringBuilder toDdl(String dbType, StringBuilder buffer) {
    buffer.append("CREATE TABLE ").append(ASTHelper.quoted(dbType, name)).append(" (");
    return joining(", ", columns.values(), buffer, (it, builder) -> it.toDdl(dbType, builder))
        .append(");");
  }
}
