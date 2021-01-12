package sjtu.ipads.wtune.superopt.rules;

import sjtu.ipads.wtune.superopt.core.Graph;
import sjtu.ipads.wtune.superopt.util.Helper;

public interface Rule {
  boolean match(Graph g);

  static boolean match(Class<? extends Rule> rule, Graph g) {
    return Helper.newInstance(rule).match(g);
  }
}
