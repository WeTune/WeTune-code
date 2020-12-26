package sjtu.ipads.wtune.symsolver.search.impl;

import sjtu.ipads.wtune.symsolver.search.Decision;
import sjtu.ipads.wtune.symsolver.search.DecisionTree;
import sjtu.ipads.wtune.symsolver.search.SearchCtx;
import sjtu.ipads.wtune.symsolver.search.Searcher;

import java.util.Arrays;

public class SearcherImpl implements Searcher {
  private final SearchCtx ctx;
  private final IntList guards;
  private final SearchStat stat;

  private SearcherImpl(SearchCtx ctx) {
    this.ctx = ctx;
    this.guards = new IntList(64);
    this.stat = new SearchStat();
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
      if (canSkip(seed)) {
        ++stat.numSkipped;
        continue;
      }

      final Decision[] decisions = tree.decisions();
      ctx.decide(decisions);

      if (ctx.isConflict()) {
        ++stat.numConflict;

      } else if (ctx.isIncomplete()) {
        ++stat.numIncomplete;
        guards.add(seed);

      } else if (ctx.prove()) {
        ++stat.numNonEq;
        guards.add(seed);

      } else {
        ++stat.numEq;
        ctx.record();
      }

      ctx.statistic().compute("searcher", (k, v) -> stat.merge((SearchStat) v));
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

  private static class SearchStat {
    private int numSearched = 0;
    private int numSkipped = 0;
    private int numConflict = 0;
    private int numIncomplete = 0;
    private int numNonEq = 0;
    private int numEq = 0;

    private SearchStat merge(SearchStat other) {
      if (other != null) {
        numSearched += other.numSearched;
        numSkipped += other.numSkipped;
        numConflict += other.numConflict;
        numIncomplete += other.numIncomplete;
        numNonEq += other.numNonEq;
        numEq += other.numEq;
      }

      return this;
    }

    @Override
    public String toString() {
      return "#Searched="
          + numSearched
          + " #Skipped="
          + numSkipped
          + " #Conflict="
          + numConflict
          + " #Incomplete="
          + numIncomplete
          + " #NonEq="
          + numNonEq
          + " #Eq="
          + numEq;
    }
  }
}
