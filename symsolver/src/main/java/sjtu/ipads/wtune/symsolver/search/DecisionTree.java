package sjtu.ipads.wtune.symsolver.search;

import com.google.common.collect.Iterables;
import sjtu.ipads.wtune.symsolver.search.impl.DecisionTreeImpl;

public interface DecisionTree {
  static DecisionTree from(Decision... choices) {
    return DecisionTreeImpl.build(choices);
  }

  static DecisionTree from(Iterable<? extends Decision> choices) {
    return from(Iterables.toArray(choices, Decision.class));
  }

  boolean forward();

  int seed();

  Decision[] decisions();

  Decision[] choices();
}
