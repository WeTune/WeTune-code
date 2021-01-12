package sjtu.ipads.wtune.symsolver.search;

import com.google.common.collect.Iterables;
import sjtu.ipads.wtune.symsolver.DecidableConstraint;
import sjtu.ipads.wtune.symsolver.search.impl.DecisionTreeImpl;

public interface DecisionTree {
  static DecisionTree from(DecidableConstraint... choices) {
    return DecisionTreeImpl.build(choices);
  }

  static DecisionTree from(Iterable<? extends DecidableConstraint> choices) {
    return from(Iterables.toArray(choices, DecidableConstraint.class));
  }

  boolean forward();

  long seed();

  Decision[] decisions();

  Decision[] choices();
}
