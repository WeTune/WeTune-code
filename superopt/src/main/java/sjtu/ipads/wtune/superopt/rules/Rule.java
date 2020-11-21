package sjtu.ipads.wtune.superopt.rules;

import sjtu.ipads.wtune.superopt.Graph;
import sjtu.ipads.wtune.superopt.Helper;

public interface Rule {
  boolean match(Graph g);

  static boolean match(Class<? extends Rule> rule, Graph g) {
    return Helper.newInstance(rule).match(g);
  }
}
