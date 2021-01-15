package sjtu.ipads.wtune.sqlparser.rel.internal;

import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.ConstraintType;
import sjtu.ipads.wtune.sqlparser.ast.constants.IndexType;
import sjtu.ipads.wtune.sqlparser.ast.constants.KeyDirection;
import sjtu.ipads.wtune.sqlparser.rel.Column;
import sjtu.ipads.wtune.sqlparser.rel.Constraint;

import java.util.List;

public class ConstraintImpl implements Constraint {

  private final ConstraintType type;
  private final List<? extends Column> columns;
  private List<KeyDirection> directions;
  private IndexType indexType;

  private SQLNode refTableName;
  private List<SQLNode> refColNames;

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
  public SQLNode refTableName() {
    return refTableName;
  }

  @Override
  public List<SQLNode> refColNames() {
    return refColNames;
  }

  void setRefTableName(SQLNode refTableName) {
    this.refTableName = refTableName;
  }

  void setRefColNames(List<SQLNode> refColNames) {
    this.refColNames = refColNames;
  }

  void setIndexType(IndexType indexType) {
    this.indexType = indexType;
  }

  void setDirections(List<KeyDirection> directions) {
    this.directions = directions;
  }
}
