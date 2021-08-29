package sjtu.ipads.wtune.superopt.optimizer;

import sjtu.ipads.wtune.sqlparser.plan.FilterNode;
import sjtu.ipads.wtune.sqlparser.plan.JoinNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;

import static sjtu.ipads.wtune.sqlparser.plan.PlanSupport.disambiguate;
import static sjtu.ipads.wtune.superopt.optimizer.OptimizerSupport.*;

class PlanNormalizer {
  static PlanNode normalize(PlanNode node) {
    final PlanNode newNode = disambiguate(inferenceInnerJoin(normalize0(node, true)));
    assert newNode.context().validate();
    newNode.freeze();
    return newNode;
  }

  private static PlanNode normalize0(PlanNode node, boolean isRoot) {
    PlanNode newNode = node;
    for (int i = 0, bound = node.kind().numPredecessors(); i < bound; i++)
      newNode = normalize0(newNode.predecessors()[0], false);
    assert newNode.kind() == node.kind();

    final PlanNode successor = newNode.successor();
    final PlanNode newNode2;
    switch (newNode.kind()) {
      case PROJ:
        newNode2 = removeDedupIfNeed(removeProjIfNeed(newNode));
        break;
      case INNER_JOIN:
      case LEFT_JOIN:
        if (successor != null && successor.kind().isJoin()) newNode2 = newNode;
        else newNode2 = normalizeJoinTree((JoinNode) newNode);
        break;
      case SIMPLE_FILTER:
      case IN_SUB_FILTER:
      case EXISTS_FILTER:
        if (successor != null && successor.kind().isFilter()) newNode2 = newNode;
        else newNode2 = insertProjIfNeed(normalizeFilterChain((FilterNode) newNode));
        break;
      default:
        newNode2 = newNode;
    }

    return isRoot ? newNode2 : newNode2.successor();
  }
}
