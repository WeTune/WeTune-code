package sjtu.ipads.wtune.symsolver.search.impl;

import sjtu.ipads.wtune.symsolver.core.Result;
import sjtu.ipads.wtune.symsolver.search.DecisionTree;
import sjtu.ipads.wtune.symsolver.search.SearchCtx;
import sjtu.ipads.wtune.symsolver.search.Searcher;

import java.util.Arrays;

public class SearcherImpl implements Searcher {
  private final SearchCtx ctx;
  private final IntList guards;

  private final long timeout;

  private int numSearched;
  private int numNotSkipped;

  private SearcherImpl(SearchCtx ctx, long timeout) {
    this.ctx = ctx;
    this.guards = new IntList(64);
    this.timeout = timeout;
  }

  public static Searcher build(SearchCtx ctx) {
    return new SearcherImpl(ctx, -1);
  }

  public static Searcher build(SearchCtx ctx, long timeout) {
    return new SearcherImpl(ctx, timeout);
  }

  @Override
  public void search(DecisionTree tree) {
    guards.clear();
    ctx.prepare(tree.choices());

    //    final long total = tree.total();
    final long start = System.currentTimeMillis();
    while (tree.forward()) {
      if (timeout >= 0 && System.currentTimeMillis() - start > timeout) return;

      ++numSearched;
      //      if (numSearched % 100000 == 0) System.out.println(numSearched + " / " + total);
      final long seed = tree.seed();
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

  private boolean canSkip(long seed) {
    for (int i = 0, bound = guards.length(); i < bound; i++)
      if ((guards.at(i) & seed) == seed) return true;
    return false;
  }

  private static class IntList {
    private long[] data;
    private int cursor;

    private IntList(int initSize) {
      data = new long[initSize];
      cursor = 0;
    }

    private int length() {
      return cursor;
    }

    private long at(int idx) {
      return data[idx];
    }

    private void add(long i) {
      if (cursor == data.length) data = Arrays.copyOf(data, data.length << 1);
      data[cursor++] = i;
    }

    private void clear() {
      cursor = 0;
    }
  }
}
