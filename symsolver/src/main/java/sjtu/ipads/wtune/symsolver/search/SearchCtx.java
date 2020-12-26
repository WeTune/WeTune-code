package sjtu.ipads.wtune.symsolver.search;

import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.TableSym;
import sjtu.ipads.wtune.symsolver.search.impl.SearchCtxImpl;
import sjtu.ipads.wtune.symsolver.smt.Proposition;
import sjtu.ipads.wtune.symsolver.smt.SmtCtx;

import java.util.List;
import java.util.Map;

public interface SearchCtx extends Tracer, Prover {
  List<Summary> search(Iterable<DecisionTree> trees);

  Map<String, Object> statistic();

  void record();

  static SearchCtx make(TableSym[] tables, PickSym[] picks, SmtCtx smtCtx, Proposition... problems) {
    return SearchCtxImpl.build(tables, picks, smtCtx, problems);
  }
}
