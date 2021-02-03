package sjtu.ipads.wtune.superopt.util.rules;

import sjtu.ipads.wtune.superopt.plan.Plan;
import sjtu.ipads.wtune.superopt.util.Helper;

public interface Rule {
  boolean match(Plan g);

  static boolean match(Class<? extends Rule> rule, Plan g) {
    return Helper.newInstance(rule).match(g);
  }
}
