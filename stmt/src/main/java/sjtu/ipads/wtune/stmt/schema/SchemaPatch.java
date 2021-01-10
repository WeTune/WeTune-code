package sjtu.ipads.wtune.stmt.schema;

import sjtu.ipads.wtune.sqlparser.ast.constants.ConstraintType;
import sjtu.ipads.wtune.stmt.dao.SchemaPatchDao;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static sjtu.ipads.wtune.common.utils.FuncUtils.coalesce;
import static sjtu.ipads.wtune.common.utils.FuncUtils.collectionMap;
import static sjtu.ipads.wtune.stmt.schema.Column.COLUMN_IS_BOOLEAN;
import static sjtu.ipads.wtune.stmt.schema.Column.COLUMN_IS_ENUM;

public class SchemaPatch {
  public enum Type {
    BOOLEAN,
    ENUM,
    UNIQUE,
    FOREIGN_KEY,
    INDEX
  }

  private String app;
  private Type type;
  private String tableName;
  private List<String> columnNames;
  private String source;

  public static SchemaPatch impliedFK(String appName, Column column) {
    final SchemaPatch patch = new SchemaPatch();
    patch.setApp(appName);
    patch.setType(Type.FOREIGN_KEY);
    patch.setTableName(column.table().tableName());
    patch.setColumnNames(Collections.singletonList(column.columnName()));
    return patch;
  }

  public static List<SchemaPatch> findByApp(String appName) {
    return SchemaPatchDao.instance().findByApp(appName);
  }

  public void save() {
    SchemaPatchDao.instance().save(this);
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

  public String source() {
    return coalesce(source, "unknown");
  }

  public SchemaPatch setApp(String app) {
    this.app = app;
    return this;
  }

  public void setSource(String source) {
    this.source = source;
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
    final Set<Column> columns = collectionMap(table::getColumn, columnNames, LinkedHashSet::new);
    if (type == Type.BOOLEAN) columns.forEach(it -> it.flag(COLUMN_IS_BOOLEAN));
    else if (type == Type.ENUM) columns.forEach(it -> it.flag(COLUMN_IS_ENUM));
    else if (type == Type.UNIQUE) {
      final Constraint c = new Constraint();
      c.setType(ConstraintType.UNIQUE);
      c.setColumns(columns);
      c.setFromPatch(true);
      table.addConstraint(c);
      columns.forEach(it -> it.addConstraint(c));

    } else if (type == Type.FOREIGN_KEY) {
      final Constraint c = new Constraint();
      c.setType(ConstraintType.FOREIGN);
      c.setColumns(columns);
      c.setFromPatch(true);
      table.addConstraint(c);
      columns.forEach(it -> it.addConstraint(c));
    }
  }
}
