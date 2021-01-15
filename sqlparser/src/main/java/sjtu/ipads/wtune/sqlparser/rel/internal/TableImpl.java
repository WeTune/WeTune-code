package sjtu.ipads.wtune.sqlparser.rel.internal;

import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.rel.Column;
import sjtu.ipads.wtune.sqlparser.rel.Constraint;
import sjtu.ipads.wtune.sqlparser.rel.Table;

import java.util.*;

import static sjtu.ipads.wtune.common.utils.FuncUtils.coalesce;
import static sjtu.ipads.wtune.sqlparser.ast.NodeAttrs.*;
import static sjtu.ipads.wtune.sqlparser.ast.SQLNode.POSTGRESQL;

public class TableImpl implements Table {
  private final String schema;
  private final String name;
  private final String engine;
  private final Map<String, ColumnImpl> columns;
  private List<Constraint> constraints;

  public TableImpl(String schema, String name, String engine) {
    this.schema = schema;
    this.name = name;
    this.engine = engine;
    this.columns = new LinkedHashMap<>();
  }

  public static TableImpl build(SQLNode tableDef) {
    final SQLNode tableName = tableDef.get(CREATE_TABLE_NAME);
    final String schema = tableName.get(TABLE_NAME_SCHEMA);
    final String name = tableDef.get(TABLE_NAME_TABLE);
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
  public Collection<? extends Column> columns() {
    return columns.values();
  }

  @Override
  public Collection<Constraint> constraints() {
    return constraints;
  }

  @Override
  public ColumnImpl column(String name) {
    return columns.get(name);
  }

  void addColumn(ColumnImpl column) {
    columns.put(column.name(), column);
  }

  void addConstraint(Constraint constraint) {
    if (constraints == null) constraints = new ArrayList<>();
    constraints.add(constraint);
  }
}
