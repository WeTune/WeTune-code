package sjtu.ipads.wtune.stmt.analyzer;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLVisitor;

import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.NODE_ID;

public class NodeFinder implements Analyzer<SQLNode>, SQLVisitor {
  private final Long targetId;
  private SQLNode found = null;

  public NodeFinder(Long targetId) {
    this.targetId = targetId;
  }

  @Override
  public boolean enter(SQLNode node) {
    if (found != null) return false;
    if (targetId.equals(node.get(NODE_ID))) {
      found = node;
      return false;
    }
    return true;
  }

  @Override
  public SQLNode analyze(SQLNode node) {
    if (targetId == null) return null;
    node.accept(this);
    return found;
  }
}
