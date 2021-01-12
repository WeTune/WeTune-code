package sjtu.ipads.wtune.symsolver.search;

import sjtu.ipads.wtune.symsolver.search.impl.SearcherImpl;

public interface Searcher {
  static Searcher bindTo(SearchCtx ctx) {
    return SearcherImpl.build(ctx);
  }

  static Searcher bindTo(SearchCtx ctx, long timeout) {
    return SearcherImpl.build(ctx, timeout);
  }

  void search(DecisionTree tree);

  int numSearched();

  int numSkipped();
}
