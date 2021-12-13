package sjtu.ipads.wtune.superopt.substitution;

import sjtu.ipads.wtune.sqlparser.plan1.PlanContext;

import java.util.Collections;
import java.util.Set;

import static sjtu.ipads.wtune.superopt.substitution.SubstitutionSupport.translateAsPlan;

public class DuplicationChecker {
  static void removeIfDuplicated(SubstitutionBank bank, Substitution sub) {
    final PlanContext plan = translateAsPlan(sub).getLeft();

    try {
      final Set<String> optimized0 = Collections.emptySet(); // TODO
      bank.remove(sub);
      final Set<String> optimized1 = Collections.emptySet(); // TODO
      optimized0.remove(plan.toString());
      optimized1.remove(plan.toString());

      if (optimized1.isEmpty() || !optimized1.containsAll(optimized0)) bank.add(sub);

    } catch (Throwable ex) {
      bank.remove(sub);
      System.out.println(sub);
    }
  }
}
