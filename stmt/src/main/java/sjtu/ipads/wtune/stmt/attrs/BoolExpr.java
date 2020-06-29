package sjtu.ipads.wtune.stmt.attrs;

import sjtu.ipads.wtune.sqlparser.SQLNode;

public class BoolExpr {
  private boolean isPrimitive;
  private SQLNode node;
  private boolean isJoinCondtion;

  public boolean isPrimitive() {
    return isPrimitive;
  }

  public boolean isJoinCondtion() {
    return isJoinCondtion;
  }

  public SQLNode node() {
    return node;
  }

  public void setJoinCondtion(boolean joinCondtion) {
    isJoinCondtion = joinCondtion;
  }

  public void setPrimitive(boolean primitive) {
    isPrimitive = primitive;
  }

  public void setNode(SQLNode node) {
    this.node = node;
  }
}
