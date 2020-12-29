package sjtu.ipads.wtune.symsolver.search.impl;

import sjtu.ipads.wtune.symsolver.core.Result;
import sjtu.ipads.wtune.symsolver.search.DecisionTree;
import sjtu.ipads.wtune.symsolver.search.SearchCtx;
import sjtu.ipads.wtune.symsolver.search.Searcher;

import java.util.Arrays;

public class SearcherImpl implements Searcher {
  private final SearchCtx ctx;
  private final IntList guards;

  private int numSearched;
  private int numNotSkipped;

  private SearcherImpl(SearchCtx ctx) {
    this.ctx = ctx;
    this.guards = new IntList(64);
  }

  public static Searcher build(SearchCtx ctx) {
    return new SearcherImpl(ctx);
  }

  @Override
  public void search(DecisionTree tree) {
    guards.clear();
    ctx.prepare(tree.choices());

    while (tree.forward()) {
      ++numSearched;
      final int seed = tree.seed();
      if (canSkip(seed)) continue;

      ++numNotSkipped;
      ctx.decide(tree.decisions());

      if (!ctx.isConflict()) {
        final Result res;
        if (ctx.isIncomplete() || (res = ctx.prove()) == Result.NON_EQUIVALENT) guards.add(seed);
        else if (res == Result.EQUIVALENT) ctx.record();
      }
    }
  }

  @Override
  public int numSearched() {
    return numSearched;
  }

  @Override
  public int numSkipped() {
    return numSearched - numNotSkipped;
  }

  private boolean canSkip(int seed) {
    for (int i = 0, bound = guards.length(); i < bound; i++)
      if ((guards.at(i) & seed) == seed) return true;
    return false;
  }

  private static class IntList {
    private int[] data;
    private int cursor;

    private IntList(int initSize) {
      data = new int[initSize];
      cursor = 0;
    }

    private int length() {
      return cursor;
    }

    private int at(int idx) {
      return data[idx];
    }

    private void add(int i) {
      if (cursor == data.length) data = Arrays.copyOf(data, data.length << 1);
      data[cursor++] = i;
    }

    private void clear() {
      cursor = 0;
    }
  }
}
