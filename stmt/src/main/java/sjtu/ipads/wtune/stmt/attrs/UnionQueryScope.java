package sjtu.ipads.wtune.stmt.attrs;

import sjtu.ipads.wtune.sqlparser.SQLNode;

public class UnionQueryScope extends QueryScope {
  private SQLNode secondSpecNode;

  public SQLNode secondSpecNode() {
    return secondSpecNode;
  }

  public void setSecondSpecNode(SQLNode secondSpecNode) {
    this.secondSpecNode = secondSpecNode;
  }
}
