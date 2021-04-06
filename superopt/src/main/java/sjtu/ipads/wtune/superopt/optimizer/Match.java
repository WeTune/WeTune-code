package sjtu.ipads.wtune.superopt.optimizer;

import static sjtu.ipads.wtune.sqlparser.plan.PlanNode.copyToRoot;
import static sjtu.ipads.wtune.sqlparser.plan.PlanNode.resolveUsedOnTree;
import static sjtu.ipads.wtune.sqlparser.plan.PlanNode.rootOf;

import sjtu.ipads.wtune.sqlparser.plan.PlanException;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Interpretations;
import sjtu.ipads.wtune.superopt.optimizer.internal.MatchImpl;

public interface Match {
  PlanNode matchPoint();

  Interpretations assignments();

  default PlanNode substitute(Fragment fragment) {
    final PlanNode newNode = fragment.instantiate(assignments());
    if (matchPoint().successor() == null) {
      resolveUsedOnTree(newNode);
      return newNode;

    } else {
      final PlanNode matchPoint = copyToRoot(matchPoint());
      matchPoint.successor().replacePredecessor(matchPoint, newNode);

      try {
        resolveUsedOnTree(rootOf(newNode));
      } catch (PlanException ex) {
        return null;
      }

      if (!matchPoint.type().isFilter()) return newNode;

      // always returns the filter chain's head
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
