package sjtu.ipads.wtune.stmt.attrs;

import sjtu.ipads.wtune.sqlparser.SQLExpr;
import sjtu.ipads.wtune.sqlparser.SQLNode;

import static sjtu.ipads.wtune.sqlparser.SQLExpr.binary;
import static sjtu.ipads.wtune.sqlparser.SQLExpr.columnRef;

public class JoinCondition {
  private final Relation left;
  private final Relation right;
  private String leftColumn;
  private String rightColumn;

  private JoinCondition(Relation left, Relation right, String leftColumn, String rightColumn) {
    this.left = left;
    this.right = right;
    this.leftColumn = leftColumn;
    this.rightColumn = rightColumn;
  }

  public static JoinCondition of(
      Relation left, Relation right, String leftColumn, String rightColumn) {
    assert left != null && right != null && leftColumn != null && rightColumn != null;

    return new JoinCondition(left, right, leftColumn, rightColumn);
  }

  public Relation left() {
    return left;
  }

  public Relation right() {
    return right;
  }

  public String leftColumn() {
    return leftColumn;
  }

  public String rightColumn() {
    return rightColumn;
  }

  public void setLeftColumn(String leftColumn) {
    this.leftColumn = leftColumn;
  }

  public void setRightColumn(String rightColumn) {
    this.rightColumn = rightColumn;
  }

  public SQLNode toBinary() {
    return binary(
        columnRef(left.name(), leftColumn),
        columnRef(right.name(), rightColumn),
        SQLExpr.BinaryOp.EQUAL);
  }

  @Override
  public String toString() {
    return String.format("<%s.%s = %s.%s>", left.name(), leftColumn, right.name(), rightColumn);
  }
}
