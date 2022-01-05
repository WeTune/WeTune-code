package sjtu.ipads.wtune.sql.support.locator;

import sjtu.ipads.wtune.sql.ast1.SqlNode;

import static sjtu.ipads.wtune.common.tree.TreeContext.NO_SUCH_NODE;

public interface SqlFinder {
  int find(SqlNode root);

  default SqlNode findNode(SqlNode root) {
    final int found = find(root);
    return found == NO_SUCH_NODE ? null : SqlNode.mk(root.context(), found);
  }
}
