package sjtu.ipads.wtune.superopt.optimizer;

import static sjtu.ipads.wtune.sqlparser.plan.PlanNode.resolveUsedOnTree;

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
    if (matchPoint().successor() == null) return newNode;
    else {
      final PlanNode matchPoint = PlanNode.copyToRoot(matchPoint());
      matchPoint.successor().replacePredecessor(matchPoint, newNode);

      try {
        resolveUsedOnTree(PlanNode.rootOf(newNode));
      } catch (PlanException ex) {
        return null;
      }

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
