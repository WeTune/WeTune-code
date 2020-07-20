package sjtu.ipads.wtune.stmt.schema;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.dao.internal.SchemaPatchDaoInstance;

import java.util.Collections;
import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.stmt.schema.Column.COLUMN_IS_BOOLEAN;
import static sjtu.ipads.wtune.stmt.schema.Column.COLUMN_IS_ENUM;

public class SchemaPatch {
  public enum Type {
    BOOLEAN,
    ENUM,
    UNIQUE,
    FOREIGN_KEY;
  }

  public static final String KEY_APP = "app";
  public static final String KEY_TYPE = "type";
  public static final String KEY_TABLE_NAME = "tableName";
  public static final String KEY_COLUMNS = "columnNames";

  private String app;
  private Type type;
  private String tableName;
  private List<String> columnNames;

  public static SchemaPatch impliedFK(String appName, Column column) {
    final SchemaPatch patch = new SchemaPatch();
    patch.setApp(appName);
    patch.setType(Type.FOREIGN_KEY);
    patch.setTableName(column.table().tableName());
    patch.setColumnNames(Collections.singletonList(column.columnName()));
    return patch;
  }

  public static List<SchemaPatch> findByApp(String appName) {
    return SchemaPatchDaoInstance.findByApp(appName);
  }

  public void save() {
    SchemaPatchDaoInstance.save(this);
  }

  public List<String> columnNames() {
    return columnNames;
  }

  public String app() {
    return app;
  }

  public String tableName() {
    return tableName;
  }

  public Type type() {
    return type;
  }

  public void setApp(String app) {
    this.app = app;
  }

  public void setType(Type type) {
    this.type = type;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  public void setColumnNames(List<String> columnNames) {
    this.columnNames = columnNames;
  }

  public void patch(Table table) {
    final List<Column> columns = listMap(table::getColumn, columnNames);
    if (type == Type.BOOLEAN) columns.forEach(it -> it.flag(COLUMN_IS_BOOLEAN));
    else if (type == Type.ENUM) columns.forEach(it -> it.flag(COLUMN_IS_ENUM));
    else if (type == Type.UNIQUE) {
      final Constraint c = new Constraint();
      c.setType(SQLNode.ConstraintType.UNIQUE);
      c.setColumns(columns);
      table.addConstraint(c);
      columns.forEach(it -> it.addConstraint(c));

    } else if (type == Type.FOREIGN_KEY) {
      final Constraint c = new Constraint();
      c.setType(SQLNode.ConstraintType.FOREIGN);
      c.setColumns(columns);
      table.addConstraint(c);
      columns.forEach(it -> it.addConstraint(c));
    }
  }
}
