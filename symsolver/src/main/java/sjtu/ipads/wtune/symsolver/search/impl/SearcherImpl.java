package sjtu.ipads.wtune.symsolver.search.impl;

import gnu.trove.list.array.TLongArrayList;
import sjtu.ipads.wtune.symsolver.core.Result;
import sjtu.ipads.wtune.symsolver.search.DecisionTree;
import sjtu.ipads.wtune.symsolver.search.SearchCtx;
import sjtu.ipads.wtune.symsolver.search.Searcher;

public class SearcherImpl implements Searcher {
  private final SearchCtx ctx;
  private final TLongArrayList guards;

  private final long timeout;

  private int numSearched;
  private int numNotSkipped;

  private SearcherImpl(SearchCtx ctx, long timeout) {
    this.ctx = ctx;
    this.guards = new TLongArrayList();
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
    //    System.out.println(tree.total());
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
    for (int i = 0, bound = guards.size(); i < bound; i++)
      if ((guards.get(i) & seed) == seed) return true;
    return false;
  }
}
