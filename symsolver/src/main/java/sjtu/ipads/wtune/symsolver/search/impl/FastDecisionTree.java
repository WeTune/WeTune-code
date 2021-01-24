package sjtu.ipads.wtune.symsolver.search.impl;

import sjtu.ipads.wtune.symsolver.DecidableConstraint;
import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.search.Decision;
import sjtu.ipads.wtune.symsolver.search.DecisionTree;
import sjtu.ipads.wtune.symsolver.utils.Partitioner;

import static java.util.Objects.requireNonNull;
import static java.util.function.Predicate.not;
import static sjtu.ipads.wtune.common.utils.Commons.*;
import static sjtu.ipads.wtune.common.utils.FuncUtils.arrayFilter;

public class FastDecisionTree implements DecisionTree {
  private final DecidableConstraint[] choices;

  private final int pickBase;
  private final int predBase;
  private final int otherBase;

  private final Partitioner pTable;
  private final Partitioner pPick;
  private final Partitioner pPred;
  private final DecisionTree tree;

  private long seed;
  private DecidableConstraint[] decisions;

  private FastDecisionTree(
      int numTables, int numPicks, int numPreds, DecidableConstraint[] choices) {
    this.choices = sorted(choices, Constraint::compareTo);

    pickBase = (numTables * (numTables - 1)) >> 1;
    predBase = pickBase + ((numPicks * (numPicks - 1)) >> 1);
    otherBase = predBase + ((numPreds * (numPreds - 1)) >> 1);

    pTable = new Partitioner(numTables);
    pPick = new Partitioner(numPicks);
    pPred = new Partitioner(numPreds);
    tree = DecisionTree.from(subArray(choices, otherBase));

    seed = -1;
  }

  public static DecisionTree build(
      int numTables, int numPicks, int numPreds, DecidableConstraint[] choices) {
    requireNonNull(choices);
    choices = arrayFilter(not(Decision::ignorable), choices);

    if (choices.length > 63) return null;
    // TODO: support arbitrary sized choices
    return new FastDecisionTree(numTables, numPicks, numPreds, choices);
  }

  @Override
  public long total() {
    return tree.total() * pTable.numPartitions() * pPick.numPartitions() * pPred.numPartitions();
  }

  @Override
  public void reset() {
    seed = -1;
    decisions = null;
    pTable.reset();
    pPick.reset();
    pPred.reset();
    tree.reset();
  }

  @Override
  public boolean forward() {
    if (seed == 0) return false;

    seed = -1;
    decisions = null;

    if (tree.forward()) return true;
    tree.reset();
    tree.forward();

    if (pPred.forward()) return true;
    pPred.reset();

    if (pPick.forward()) return true;
    pPick.reset();

    if (pTable.forward()) return true;

    seed = 0;
    return false;
  }

  @Override
  public long seed() {
    if (seed > -1) return seed;

    final long tableSeed = partitionToSeed(pTable.cardinality(), pTable.partition());
    final long pickSeed = partitionToSeed(pPick.cardinality(), pPick.partition());
    final long predSeed = partitionToSeed(pPred.cardinality(), pPred.partition());
    final long otherSeed = tree.seed();

    final int wall = choices.length;

    seed =
        (tableSeed << (wall - pickBase))
            | (pickSeed << (wall - predBase))
            | (predSeed << (wall - otherBase))
            | otherSeed;

    assert seed >= 0;

    return seed;
  }

  @Override
  public Decision[] decisions() {
    return decisions != null ? decisions : (decisions = maskArray(choices, seed()));
  }

  @Override
  public Decision[] choices() {
    return choices;
  }

  private static long partitionToSeed(int cardinality, int[][] partition) {
    // Turn a partition of symbol set into the index of constraints.
    //
    // Given a list of symbols, a partition over these symbols is represented by a 2d array, whose
    // 1st dim are groups, and the 2nd dim are the indices of symbols in the group.
    //
    // Example:
    //   Suppose a list of symbols: [w,x,y,z].
    //   All possible equality constraints over these symbols are
    //     [Eq(w,x),Eq(w,y),Eq(w,z),Eq(x,y),Eq(x,z),Eq(y,z)]
    //
    //   [[0,2],[1,3]] represents a partition that [w,y] are in the same group (equivalent class),
    //     while [x,z] are in another.
    //
    // This function turns such partition into the index of constraints, and then a seed
    // e.g. [[0,2],[1,3]] => {Eq(w,y),Eq(x,z)} => [1,4] => 0b010010

    final int wall = (cardinality * (cardinality - 1)) >> 1;
    long seed = 0;

    for (int[] group : partition)
      if (group.length != 1)
        for (int p = 0, bound = group.length - 1; p < bound; p++)
          for (int q = p + 1, i = group[p]; q <= bound; q++) {
            final int j = group[q];
            final int index = cardinality * i - ((i * (i + 1)) >> 1) + j - (i + 1);
            assert index < wall;
            seed |= 1L << (wall - index - 1);
          }

    return seed;
  }
}
