package sjtu.ipads.wtune.superopt.util.rules;

import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.util.Helper;

public interface Rule {
  boolean match(Fragment g);

  static boolean match(Class<? extends Rule> rule, Fragment g) {
    return Helper.newInstance(rule).match(g);
  }
}
