package sjtu.ipads.wtune.superopt.optimizer;

import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Interpretations;
import sjtu.ipads.wtune.superopt.optimizer.internal.MatchImpl;

import static sjtu.ipads.wtune.sqlparser.plan.PlanNode.resolveUsedOnTree;

public interface Match {
  PlanNode matchPoint();

  Interpretations assignments();

  default PlanNode substitute(Fragment fragment) {
    final PlanNode newNode = fragment.instantiate(assignments());
    if (matchPoint().successor() == null) return newNode;
    else {
      final PlanNode matchPoint = PlanNode.copyToRoot(matchPoint());
      matchPoint.successor().replacePredecessor(matchPoint, newNode);
      resolveUsedOnTree(PlanNode.rootOf(newNode));

      if (!matchPoint.type().isFilter()) return newNode;

      PlanNode succ = newNode;
      while (succ.successor().type().isFilter()) succ = succ.successor();
      return succ;
    }
  }

  default Match lift() {
    return make(matchPoint().successor(), assignments());
  }

  static Match make(PlanNode matchPoint, Interpretations interpretations) {
    return new MatchImpl(matchPoint, interpretations);
  }
}
