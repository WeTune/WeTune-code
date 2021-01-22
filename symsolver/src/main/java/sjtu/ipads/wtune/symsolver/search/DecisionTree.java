package sjtu.ipads.wtune.symsolver.search;

import com.google.common.collect.Iterables;
import sjtu.ipads.wtune.symsolver.DecidableConstraint;
import sjtu.ipads.wtune.symsolver.search.impl.DecisionTreeImpl;
import sjtu.ipads.wtune.symsolver.search.impl.FastDecisionTree;

public interface DecisionTree {
  static DecisionTree from(DecidableConstraint... choices) {
    return DecisionTreeImpl.build(choices);
  }

  static DecisionTree from(Iterable<? extends DecidableConstraint> choices) {
    return from(Iterables.toArray(choices, DecidableConstraint.class));
  }

  static DecisionTree fast(int numTbl, int numPick, int numPred, DecidableConstraint... choices) {
    return FastDecisionTree.build(numTbl, numPick, numPred, choices);
  }

  static DecisionTree fast(
      int numTbl, int numPick, int numPred, Iterable<DecidableConstraint> choices) {
    return fast(numTbl, numPick, numPred, Iterables.toArray(choices, DecidableConstraint.class));
  }

  void reset();

  boolean forward();

  long seed();

  long total();

  Decision[] decisions();

  Decision[] choices();
}
