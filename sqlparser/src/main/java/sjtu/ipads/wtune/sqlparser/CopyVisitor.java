package sjtu.ipads.wtune.sqlparser;

import sjtu.ipads.wtune.common.attrs.Attrs;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

class CopyVisitor implements SQLVisitor {
  private final Deque<SQLNode> current = new LinkedList<>();

  @Override
  public boolean enter(SQLNode node) {
    current.push(node.copy0());
    return true;
  }

  @Override
  public boolean enterChild(SQLNode parent, Attrs.Key<SQLNode> key, SQLNode child) {
    if (child != null) {
      child.accept(this);
      final SQLNode newChild = current.pop();
      current.peek().put(key, newChild);
    }
    return false;
  }

  @Override
  public boolean enterChildren(
      SQLNode parent, Attrs.Key<List<SQLNode>> key, List<SQLNode> children) {
    if (children != null) {
      final List<SQLNode> newChildren = new ArrayList<>(children.size());

      for (SQLNode child : children) {
        if (child != null) {
          child.accept(this);
          final SQLNode newChild = current.pop();
          newChildren.add(newChild);
        } else newChildren.add(null);
      }

      current.peek().put(key, newChildren);
    }
    return false;
  }

  public static SQLNode doCopy(SQLNode node) {
    final CopyVisitor visitor = new CopyVisitor();
    node.accept(visitor);
    return visitor.current.pop().relinkAll();
  }
}
