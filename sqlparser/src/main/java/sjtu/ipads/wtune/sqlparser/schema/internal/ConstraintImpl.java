package sjtu.ipads.wtune.sqlparser.schema.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.ConstraintType;
import sjtu.ipads.wtune.sqlparser.ast.constants.IndexType;
import sjtu.ipads.wtune.sqlparser.ast.constants.KeyDirection;
import sjtu.ipads.wtune.sqlparser.schema.Column;
import sjtu.ipads.wtune.sqlparser.schema.Constraint;
import sjtu.ipads.wtune.sqlparser.schema.Table;

import java.util.List;

public class ConstraintImpl implements Constraint {

  private final ConstraintType type;
  private final List<? extends Column> columns;
  private List<KeyDirection> directions;
  private IndexType indexType;

  private ASTNode refTableName;
  private List<ASTNode> refColNames;

  private Table refTable;
  private List<Column> refColumns;

  private ConstraintImpl(ConstraintType type, List<? extends Column> columns) {
    this.type = type;
    this.columns = columns;
  }

  static ConstraintImpl build(ConstraintType type, List<? extends Column> columns) {
    return new ConstraintImpl(type, columns);
  }

  @Override
  public List<? extends Column> columns() {
    return columns;
  }

  @Override
  public List<KeyDirection> directions() {
    return directions;
  }

  @Override
  public ConstraintType type() {
    return type;
  }

  @Override
  public Table refTable() {
    return refTable;
  }

  @Override
  public List<Column> refColumns() {
    return refColumns;
  }

  ASTNode refTableName() {
    return refTableName;
  }

  List<ASTNode> refColNames() {
    return refColNames;
  }

  void setRefTable(Table refTable) {
    this.refTable = refTable;
  }

  void setRefColumns(List<Column> refColumns) {
    this.refColumns = refColumns;
  }

  void setRefTableName(ASTNode refTableName) {
    this.refTableName = refTableName;
  }

  void setRefColNames(List<ASTNode> refColNames) {
    this.refColNames = refColNames;
  }

  void setIndexType(IndexType indexType) {
    this.indexType = indexType;
  }

  void setDirections(List<KeyDirection> directions) {
    this.directions = directions;
  }
}
