package sjtu.ipads.wtune.symsolver.search.impl;

import sjtu.ipads.wtune.symsolver.DecidableConstraint;
import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.search.Decision;
import sjtu.ipads.wtune.symsolver.search.DecisionTree;

import static java.util.Objects.requireNonNull;
import static java.util.function.Predicate.not;
import static sjtu.ipads.wtune.common.utils.Commons.maskArray;
import static sjtu.ipads.wtune.common.utils.Commons.sorted;
import static sjtu.ipads.wtune.common.utils.FuncUtils.arrayFilter;

public class DecisionTreeImpl implements DecisionTree {
  private final DecidableConstraint[] choices;

  private long seed;
  private DecidableConstraint[] decisions;

  private DecisionTreeImpl(DecidableConstraint[] choices) {
    this.choices = sorted(choices, Constraint::compareTo);
    this.seed = 1L << this.choices.length;
  }

  public static DecisionTree build(DecidableConstraint[] choices) {
    requireNonNull(choices);
    choices = arrayFilter(not(Decision::ignorable), choices);

    if (choices.length > 63) return null;
    // TODO: support arbitrary sized choices
    return new DecisionTreeImpl(choices);
  }

  @Override
  public long total() {
    return 1L << choices.length;
  }

  @Override
  public void reset() {
    seed = 1L << choices.length;
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
    return decisions != null ? decisions : (decisions = maskArray(choices, seed));
  }

  @Override
  public Decision[] choices() {
    return choices;
  }
}
