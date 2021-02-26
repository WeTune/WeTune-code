package sjtu.ipads.wtune.superopt.optimization;

import sjtu.ipads.wtune.common.multiversion.Snapshot;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Interpretations;
import sjtu.ipads.wtune.superopt.optimization.internal.MatchingImpl;

import static sjtu.ipads.wtune.sqlparser.plan.PlanNode.resolveUsedTree;

public interface Match {
  PlanNode matchPoint();

  Snapshot interpretation();

  default PlanNode substitute(Fragment fragment, Interpretations inter) {
    inter.setSnapshot(interpretation());
    final PlanNode newNode = fragment.instantiate(inter);
    final PlanNode matchPoint = PlanNode.copyToRoot(matchPoint());
    if (matchPoint.successor() == null) return newNode;
    else {
      matchPoint.successor().replacePredecessor(matchPoint, newNode);
      final PlanNode root = PlanNode.rootOf(newNode);
      resolveUsedTree(root);
      return root;
    }
  }

  default Match percolateUp() {
    return build(matchPoint().successor(), interpretation());
  }

  static Match build(PlanNode matchPoint, Snapshot interpretation) {
    return MatchingImpl.build(matchPoint, interpretation);
  }
}
