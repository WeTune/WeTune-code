package sjtu.ipads.wtune.superopt.optimizer1;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan1.FilterNode;
import sjtu.ipads.wtune.sqlparser.plan1.JoinNode;
import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;

import static sjtu.ipads.wtune.sqlparser.plan1.PlanSupport.disambiguate;
import static sjtu.ipads.wtune.superopt.optimizer1.OptimizerSupport.*;

class PlanNormalizer {
  static PlanNode normalize(PlanNode node) {
    final PlanNode newPlan = disambiguate(inferenceInnerJoin(normalize0(node)));
    assert newPlan.context().validate();
    newPlan.freeze();
    return newPlan;
  }

  private static PlanNode normalize0(PlanNode node) {
    PlanNode newNode = node;
    for (int i = 0, bound = node.kind().numPredecessors(); i < bound; i++)
      newNode = normalize0(newNode.predecessors()[0]);
    assert newNode.kind() == node.kind();

    final PlanNode successor = newNode.successor();
    OperatorType kind = newNode.kind();
    switch (kind) {
      case PROJ:
        return removeDedupIfNeed(removeProjIfNeed(newNode)).successor();
      case INNER_JOIN:
      case LEFT_JOIN:
        if (successor != null && successor.kind().isJoin()) return successor;
        else return normalizeJoinTree((JoinNode) newNode).successor();
      case SIMPLE_FILTER:
      case IN_SUB_FILTER:
      case EXISTS_FILTER:
        if (successor != null && successor.kind().isFilter()) return successor;
        else return insertProjIfNeed(normalizeFilterChain((FilterNode) newNode)).successor();
      default:
        return successor;
    }
  }
}
