package sjtu.ipads.wtune.superopt.substitution;

import sjtu.ipads.wtune.sqlparser.plan.PlanNode;

import java.util.Set;

import static sjtu.ipads.wtune.common.utils.FuncUtils.setMap;
import static sjtu.ipads.wtune.sqlparser.plan.PlanSupport.resolveSubqueryExpr;
import static sjtu.ipads.wtune.superopt.optimizer.OptimizerSupport.optimize;
import static sjtu.ipads.wtune.superopt.substitution.SubstitutionSupport.translateAsPlan;

public class DuplicationChecker2 {
  static void removeIfDuplicated(SubstitutionBank bank, Substitution sub) {
    final PlanNode plan = resolveSubqueryExpr(translateAsPlan(sub, false, true).getLeft());

    try {
      final Set<String> optimized0 = setMap(optimize(bank, plan), Object::toString);
      bank.remove(sub);
      final Set<String> optimized1 = setMap(optimize(bank, plan), Object::toString);
      optimized0.remove(plan.toString());
      optimized1.remove(plan.toString());

      if (optimized1.isEmpty() || !optimized1.containsAll(optimized0)) bank.add(sub);

    } catch (Throwable ex) {
      bank.remove(sub);
      System.out.println(sub);
    }
  }
}
