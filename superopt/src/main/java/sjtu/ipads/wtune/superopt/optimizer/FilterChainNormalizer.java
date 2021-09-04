package sjtu.ipads.wtune.superopt.optimizer;

import sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp;
import sjtu.ipads.wtune.sqlparser.plan.CombinedFilterNode;
import sjtu.ipads.wtune.sqlparser.plan.Expr;
import sjtu.ipads.wtune.sqlparser.plan.FilterNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;

import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_OP;

class FilterChainNormalizer {
  static FilterNode normalize(FilterNode node) {
    return (FilterNode) normalize0(node, true);
  }

  private static PlanNode normalize0(PlanNode node, boolean isRoot) {
    if (node.kind().isFilter()
        && (node.successor() == null || !node.successor().kind().isFilter())) {
      final FilterNode chainHead = (FilterNode) node;
      if (containsCombinedFilterNode(chainHead))
        node = FilterChain.mk(chainHead, true).buildChain();
    }

    for (int i = 0, bound = node.kind().numPredecessors(); i < bound; i++)
      node = normalize0(node.predecessors()[i], false);

    return isRoot ? node : node.successor();
  }

  private static boolean containsCombinedFilterNode(FilterNode chainHead) {
    PlanNode path = chainHead;
    while (path.kind().isFilter()) {
      if (path instanceof CombinedFilterNode) return true;
      if (isCombinedExpr(((FilterNode) path).predicate())) return true;
      path = path.predecessors()[0];
    }
    return false;
  }

  private static boolean isCombinedExpr(Expr expr) {
    return expr.template().get(BINARY_OP) == BinaryOp.AND;
  }
}
