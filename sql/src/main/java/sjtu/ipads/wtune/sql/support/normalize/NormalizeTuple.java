package sjtu.ipads.wtune.sql.support.normalize;

import sjtu.ipads.wtune.sql.ast1.SqlContext;
import sjtu.ipads.wtune.sql.ast1.SqlNode;
import sjtu.ipads.wtune.sql.ast1.SqlNodes;
import sjtu.ipads.wtune.sql.ast1.constants.BinaryOpKind;

import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.common.tree.TreeSupport.nodeEquals;
import static sjtu.ipads.wtune.sql.ast1.ExprFields.*;
import static sjtu.ipads.wtune.sql.ast1.ExprKind.*;
import static sjtu.ipads.wtune.sql.ast1.constants.BinaryOpKind.ARRAY_CONTAINS;
import static sjtu.ipads.wtune.sql.ast1.constants.BinaryOpKind.IN_LIST;
import static sjtu.ipads.wtune.sql.support.locator.LocatorSupport.nodeLocator;

class NormalizeTuple {
  static void normalize(SqlNode node) {
    for (SqlNode target : nodeLocator().accept(NormalizeTuple::isTuple).gather(node))
      normalizeTuple(target);
  }

  private static boolean isTuple(SqlNode node) {
    final SqlNode parent = node.parent();
    if (parent == null) return false;

    final BinaryOpKind op = parent.$(Binary_Op);
    final SqlNode rhs = parent.$(Binary_Right);
    return (op == ARRAY_CONTAINS || op == IN_LIST) && nodeEquals(rhs, node);
  }

  private static void normalizeTuple(SqlNode node) {
    final SqlContext ctx = node.context();
    final SqlNodes elements = SqlNodes.mk(ctx, singletonList(SqlNode.mk(ctx, Param)));
    if (Array.isInstance(node)) node.$(Array_Elements, elements);
    else if (Tuple.isInstance(node)) node.setField(Tuple_Exprs, elements);
    else assert false;
  }
}
