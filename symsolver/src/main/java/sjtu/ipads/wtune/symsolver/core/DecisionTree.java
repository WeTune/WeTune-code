package sjtu.ipads.wtune.symsolver.core;

import sjtu.ipads.wtune.symsolver.core.impl.DecisionTreeImpl;

public interface DecisionTree {
  boolean forward();

  int seed();

  Decision[] decisions();

  Decision[] choices();

  static DecisionTree from(Decision... choices) {
    return DecisionTreeImpl.build(choices);
  }
}
