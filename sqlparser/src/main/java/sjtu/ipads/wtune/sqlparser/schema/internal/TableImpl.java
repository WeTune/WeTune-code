package sjtu.ipads.wtune.sqlparser.schema.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.ConstraintType;
import sjtu.ipads.wtune.sqlparser.ast.constants.NodeType;
import sjtu.ipads.wtune.sqlparser.schema.Column;
import sjtu.ipads.wtune.sqlparser.schema.Constraint;
import sjtu.ipads.wtune.sqlparser.schema.SchemaPatch;
import sjtu.ipads.wtune.sqlparser.schema.SchemaPatch.Type;
import sjtu.ipads.wtune.sqlparser.schema.Table;

import java.util.*;

import static sjtu.ipads.wtune.common.utils.Commons.coalesce;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.ast.ASTNode.POSTGRESQL;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.*;
import static sjtu.ipads.wtune.sqlparser.util.ASTHelper.simpleName;

public class TableImpl implements Table {
  private final String schema;
  private final String name;
  private final String engine;
  private final Map<String, Column> columns;
  private List<Constraint> constraints;

  public TableImpl(String schema, String name, String engine) {
    this.schema = schema;
    this.name = name;
    this.engine = engine;
    this.columns = new LinkedHashMap<>();
  }

  public static TableImpl build(ASTNode tableDef) {
    final ASTNode tableName = tableDef.get(CREATE_TABLE_NAME);
    final String schema = simpleName(tableName.get(TABLE_NAME_SCHEMA));
    final String name = simpleName(tableName.get(TABLE_NAME_TABLE));
    final String engine =
        POSTGRESQL.equals(tableDef.dbType())
            ? POSTGRESQL
            : coalesce(tableDef.get(CREATE_TABLE_ENGINE), "innodb");

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
      final List<Column> columns = listMap(patch.columns(), this::column);
      final ConstraintImpl constraint = ConstraintImpl.build(ConstraintType.UNIQUE, columns);

      addConstraint(constraint);
      columns.forEach(it -> ((ColumnImpl) it).addConstraint(constraint));
    }

    if (patch.type() == Type.FOREIGN_KEY) {
      final List<Column> columns = listMap(patch.columns(), this::column);
      final ConstraintImpl constraint = ConstraintImpl.build(ConstraintType.FOREIGN, columns);

      final String[] split = patch.reference().split("\\.");
      if (split.length != 2) throw new IllegalArgumentException("illegal patch: " + patch);

      final ASTNode tableName = ASTNode.node(NodeType.TABLE_NAME);
      tableName.set(TABLE_NAME_TABLE, split[0]);
      constraint.setRefTableName(tableName);

      final ASTNode colName = ASTNode.node(NodeType.COLUMN_NAME);
      colName.set(COLUMN_NAME_COLUMN, split[1]);
      constraint.setRefColNames(Collections.singletonList(colName));

      addConstraint(constraint);
      columns.forEach(it -> ((ColumnImpl) it).addConstraint(constraint));
    }
  }

  void addColumn(ColumnImpl column) {
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
}
