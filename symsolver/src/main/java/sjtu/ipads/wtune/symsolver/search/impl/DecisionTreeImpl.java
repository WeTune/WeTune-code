package sjtu.ipads.wtune.symsolver.search.impl;

import sjtu.ipads.wtune.symsolver.search.Decision;
import sjtu.ipads.wtune.symsolver.search.DecisionTree;

import static java.util.Objects.requireNonNull;

public class DecisionTreeImpl implements DecisionTree {
  private final Decision[] choices;

  private int seed;
  private Decision[] decisions;

  private DecisionTreeImpl(Decision[] choices) {
    this.choices = choices;
    this.seed = 1 << choices.length;
  }

  public static DecisionTree build(Decision[] choices) {
    requireNonNull(choices);
    return new DecisionTreeImpl(choices);
  }

  private static Decision[] toDecision(Decision[] choices, int seed) {
    final Decision[] decisions = new Decision[Integer.bitCount(seed)];

    final int wall = choices.length - 1;
    for (int i = 0, j = 0, bound = choices.length; i < bound; i++)
      if ((seed & (1 << (wall - i))) != 0) decisions[j++] = choices[i];

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
  public int seed() {
    return seed;
  }

  @Override
  public Decision[] decisions() {
    return decisions != null ? decisions : (decisions = toDecision(choices, seed));
  }

  @Override
  public Decision[] choices() {
    return choices;
  }
}
