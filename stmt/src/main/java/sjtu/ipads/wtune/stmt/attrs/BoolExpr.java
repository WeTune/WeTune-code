package sjtu.ipads.wtune.stmt.attrs;

import sjtu.ipads.wtune.sqlparser.SQLNode;

import java.util.Objects;

import static sjtu.ipads.wtune.stmt.utils.StmtHelper.nodeEquals;
import static sjtu.ipads.wtune.stmt.utils.StmtHelper.nodeHash;

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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BoolExpr boolExpr = (BoolExpr) o;
    return nodeEquals(node, boolExpr.node);
  }

  @Override
  public int hashCode() {
    return nodeHash(node);
  }
}
