package sjtu.ipads.wtune.symsolver.search.impl;

import sjtu.ipads.wtune.symsolver.DecidableConstraint;
import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.search.Decision;
import sjtu.ipads.wtune.symsolver.search.DecisionTree;

import static java.util.Objects.requireNonNull;
import static java.util.function.Predicate.not;
import static sjtu.ipads.wtune.common.utils.FuncUtils.arrayFilter;
import static sjtu.ipads.wtune.common.utils.FuncUtils.sorted;

public class DecisionTreeImpl implements DecisionTree {
  private final DecidableConstraint[] choices;

  private long seed;
  private DecidableConstraint[] decisions;

  private DecisionTreeImpl(DecidableConstraint[] choices) {
    this.choices = arrayFilter(not(Decision::ignorable), sorted(choices, Constraint::compareTo));
    this.seed = 1L << this.choices.length;
  }

  public static DecisionTree build(DecidableConstraint[] choices) {
    requireNonNull(choices);
    return new DecisionTreeImpl(choices);
  }

  private static DecidableConstraint[] toDecision(DecidableConstraint[] choices, long seed) {
    final DecidableConstraint[] decisions = new DecidableConstraint[Long.bitCount(seed)];

    final int wall = choices.length - 1;
    for (int i = 0, j = 0, bound = choices.length; i < bound; i++)
      if ((seed & (1L << (wall - i))) != 0) decisions[j++] = choices[i];

    return decisions;
  }

  @Override
  public boolean forward() {
    if (seed > 0) {
      --seed;
      decisions = null;
      return true;
    } else return false;
  }

  @Override
  public long seed() {
    return seed;
  }

  @Override
  public DecidableConstraint[] decisions() {
    return decisions != null ? decisions : (decisions = toDecision(choices, seed));
  }

  @Override
  public Decision[] choices() {
    return choices;
  }
}
