package sjtu.ipads.wtune.stmt.schema;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLNode.ConstraintType;
import sjtu.ipads.wtune.sqlparser.SQLNode.IndexType;
import sjtu.ipads.wtune.sqlparser.SQLNode.KeyDirection;

import java.util.List;

public class Constraint {
  public static class Key {
    private Column column;
    private KeyDirection direction;

    public Column column() {
      return column;
    }

    public KeyDirection direction() {
      return direction;
    }

    public void setColumn(Column column) {
      this.column = column;
    }

    public void setDirection(KeyDirection direction) {
      this.direction = direction;
    }
  }

  private List<Column> columns;
  private List<KeyDirection> directions;
  private ConstraintType type;
  private IndexType indexType;

  private SQLNode refTableName;
  private List<SQLNode> refColNames;

  private Table refTable;
  private List<Column> refColumns;

  public List<Column> columns() {
    return columns;
  }

  public Table refTable() {
    return refTable;
  }

  public List<Column> refColumns() {
    return refColumns;
  }

  public List<KeyDirection> directions() {
    return directions;
  }

  public ConstraintType type() {
    return type;
  }

  public SQLNode refTableName() {
    return refTableName;
  }

  public List<SQLNode> refColNames() {
    return refColNames;
  }

  public void setRefTableName(SQLNode refTableName) {
    this.refTableName = refTableName;
  }

  public void setRefColNames(List<SQLNode> refColNames) {
    this.refColNames = refColNames;
  }

  public void setIndexType(IndexType indexType) {
    this.indexType = indexType;
  }

  public void setColumns(List<Column> columns) {
    this.columns = columns;
  }

  public void setRefTable(Table refTable) {
    this.refTable = refTable;
  }

  public void setRefColumns(List<Column> refColumns) {
    this.refColumns = refColumns;
  }

  public void setDirections(List<KeyDirection> directions) {
    this.directions = directions;
  }

  public void setType(ConstraintType type) {
    this.type = type;
  }
}
