package sjtu.ipads.wtune.superopt.optimizer;

import sjtu.ipads.wtune.sqlparser.plan.*;

import static sjtu.ipads.wtune.common.utils.FuncUtils.zipForEach;
import static sjtu.ipads.wtune.common.utils.TreeScaffold.displaceGlobal;
import static sjtu.ipads.wtune.sqlparser.plan.PlanSupport.isWildcardProj;
import static sjtu.ipads.wtune.sqlparser.plan.PlanSupport.wrapWildcardProj;

class ProjNormalizer {
  static PlanNode insertProjIfNeed(PlanNode node) {
    if (!shouldInsertProj(node)) return node;

    final PlanNode proj = displaceGlobal(node, wrapWildcardProj(node), false);
    final PlanContext ctx = proj.context();
    final ValueBag newAttrs = proj.values();
    final ValueBag oldAttrs = node.values();
    assert newAttrs.size() == oldAttrs.size();
    zipForEach(oldAttrs, newAttrs, ctx::changeIndirection);

    return proj;
  }

  static PlanNode removeProjIfNeed(PlanNode node) {
    if (!shouldRemoveProj(node)) return node;
    final PlanNode newNode = displaceGlobal(node, node.predecessors()[0], true);

    final PlanContext ctx = newNode.context();
    assert node.kind() == OperatorType.PROJ;
    final ValueBag newAttrs = newNode.values();
    final ValueBag oldAttrs = node.values();
    assert newAttrs.size() == oldAttrs.size();
    zipForEach(oldAttrs, newAttrs, ctx::changeIndirection);

    return newNode;
  }

  static PlanNode removeDedupIfNeed(PlanNode node) {
    if (shouldRemoveDedup(node)) ((ProjNode) node).setDeduplicated(false);
    return node;
  }

  private static boolean shouldInsertProj(PlanNode node) {
    final PlanNode successor = node.successor();
    return successor != null && successor.kind().isJoin() && node.kind().isFilter();
  }

  private static boolean shouldRemoveProj(PlanNode node) {
    final PlanNode successor = node.successor();
    final PlanNode predecessor = node.predecessors()[0];
    return node.kind() == OperatorType.PROJ
        && successor != null
        && !predecessor.kind().isFilter()
        && isWildcardProj((ProjNode) node);
  }

  private static boolean shouldRemoveDedup(PlanNode node) {
    final PlanNode successor = node.successor();
    return node.kind() == OperatorType.PROJ
        && successor != null
        && successor.kind().isSubquery()
        && successor.predecessors()[1] == node;
  }
}
