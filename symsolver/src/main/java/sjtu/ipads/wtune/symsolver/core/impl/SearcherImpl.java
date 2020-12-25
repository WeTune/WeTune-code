package sjtu.ipads.wtune.symsolver.core.impl;

import sjtu.ipads.wtune.symsolver.core.Decision;
import sjtu.ipads.wtune.symsolver.core.DecisionTree;
import sjtu.ipads.wtune.symsolver.core.SearchCtx;
import sjtu.ipads.wtune.symsolver.core.Searcher;

import java.util.Arrays;

public class SearcherImpl implements Searcher {
  private final SearchCtx ctx;
  private final IntList guards;

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
      final int seed = tree.seed();
      if (canSkip(seed)) continue;

      final Decision[] decisions = tree.decisions();
      ctx.decide(decisions);

      if (ctx.isConflict()) continue;
      if (ctx.isIncomplete() || !ctx.prove()) guards.add(seed);
      else ctx.record();
    }
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
