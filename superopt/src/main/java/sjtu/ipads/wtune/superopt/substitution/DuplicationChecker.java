package sjtu.ipads.wtune.superopt.substitution;

import sjtu.ipads.wtune.common.utils.SetSupport;
import sjtu.ipads.wtune.sql.plan.PlanContext;
import sjtu.ipads.wtune.superopt.optimizer.Optimizer;

import java.util.Set;

import static sjtu.ipads.wtune.sql.plan.PlanSupport.stringifyTree;
import static sjtu.ipads.wtune.superopt.substitution.SubstitutionSupport.translateAsPlan2;

public class DuplicationChecker {
  static void removeIfDuplicated(SubstitutionBank bank, Substitution rule) {
    final PlanContext plan = translateAsPlan2(rule).getLeft();

    try {
      final String str = stringifyTree(plan, plan.root());
      final Set<String> optimized0 = optimizeAsString(plan, bank);
      bank.remove(rule);
      final Set<String> optimized1 = optimizeAsString(plan, bank);
      optimized0.remove(str);
      optimized1.remove(str);

      if (optimized1.isEmpty() || !optimized1.containsAll(optimized0)) bank.add(rule);

    } catch (Throwable ex) {
      bank.remove(rule);
      System.out.println(rule);
    }
  }

  private static Set<String> optimizeAsString(PlanContext plan, SubstitutionBank rules) {
    final Optimizer optimizer = Optimizer.mk(rules);
    return SetSupport.map(optimizer.optimize(plan), it -> stringifyTree(it, it.root()));
  }
}
