package sjtu.ipads.wtune.superopt.fragment1.pruning;

import sjtu.ipads.wtune.superopt.fragment1.Fragment;
import sjtu.ipads.wtune.superopt.util.Helper;

public interface Rule {
  boolean match(Fragment g);

  static boolean match(Class<? extends Rule> rule, Fragment g) {
    return Helper.newInstance(rule).match(g);
  }
}
