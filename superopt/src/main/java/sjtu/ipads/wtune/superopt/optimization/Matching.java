package sjtu.ipads.wtune.superopt.optimization;

import sjtu.ipads.wtune.common.multiversion.Snapshot;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Interpretations;
import sjtu.ipads.wtune.superopt.optimization.internal.MatchingImpl;

public interface Matching {
  PlanNode matchPoint();

  Snapshot interpretation();

  default PlanNode substitute(Fragment fragment, Interpretations inter) {
    inter.setSnapshot(interpretation());
    final PlanNode newNode = fragment.instantiate(inter);
    final PlanNode matchPoint = forkFromRoot(matchPoint());
    if (matchPoint.successor() == null) return newNode;
    else {
      matchPoint.successor().replacePredecessor(matchPoint, newNode);
      return traceToRoot(newNode);
    }
  }

  private static PlanNode forkFromRoot(PlanNode node) {
    // copy the nodes on the path from `node` to root
    final PlanNode copy = node.copy();
    if (node.successor() == null) return copy;

    final PlanNode successor = forkFromRoot(node.successor());
    successor.replacePredecessor(node, copy);
    return copy;
  }

  private static PlanNode traceToRoot(PlanNode node) {
    while (node.successor() != null) node = node.successor();
    return node;
  }

  static Matching build(PlanNode matchPoint, Snapshot interpretation) {
    return MatchingImpl.build(matchPoint, interpretation);
  }
}
