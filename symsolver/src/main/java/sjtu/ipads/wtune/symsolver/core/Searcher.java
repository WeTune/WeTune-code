package sjtu.ipads.wtune.symsolver.core;

import sjtu.ipads.wtune.symsolver.core.impl.SearcherImpl;

public interface Searcher {
  void search(DecisionTree tree);

  static Searcher bindTo(SearchCtx ctx) {
    return SearcherImpl.build(ctx);
  }
}
