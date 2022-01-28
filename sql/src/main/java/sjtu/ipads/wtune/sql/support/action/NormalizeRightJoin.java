package sjtu.ipads.wtune.sql.support.action;

import sjtu.ipads.wtune.sql.ast.SqlNode;
import sjtu.ipads.wtune.sql.ast.constants.JoinKind;

import static sjtu.ipads.wtune.common.tree.TreeContext.NO_SUCH_NODE;
import static sjtu.ipads.wtune.sql.ast.TableSourceFields.*;
import static sjtu.ipads.wtune.sql.ast.TableSourceKind.SimpleSource;
import static sjtu.ipads.wtune.sql.support.locator.LocatorSupport.nodeLocator;

class NormalizeRightJoin {
  static void normalize(SqlNode node) {
    for (SqlNode target : nodeLocator().accept(NormalizeRightJoin::isRightJoin).gather(node))
      flipJoin(target);
  }

  private static boolean isRightJoin(SqlNode node) {
    return node.$(Joined_Kind) == JoinKind.RIGHT_JOIN
        && SimpleSource.isInstance(node.$(Joined_Left));
  }

  private static void flipJoin(SqlNode node) {
    final SqlNode left = (SqlNode) node.remove(Joined_Left);
    final SqlNode right = (SqlNode) node.remove(Joined_Right);
    node.context().setParentOf(left.nodeId(), NO_SUCH_NODE);
    node.context().setParentOf(right.nodeId(), NO_SUCH_NODE);
    node.$(Joined_Left, right);
    node.$(Joined_Right, left);
    node.$(Joined_Kind, JoinKind.LEFT_JOIN);
  }
}
