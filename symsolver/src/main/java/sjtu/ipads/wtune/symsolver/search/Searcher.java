package sjtu.ipads.wtune.symsolver.search;

import sjtu.ipads.wtune.symsolver.search.impl.SearcherImpl;

public interface Searcher {
  static Searcher bindTo(SearchCtx ctx) {
    return SearcherImpl.build(ctx);
  }

  void search(DecisionTree tree);
}
