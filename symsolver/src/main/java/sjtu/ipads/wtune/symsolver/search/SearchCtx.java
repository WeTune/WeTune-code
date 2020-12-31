package sjtu.ipads.wtune.symsolver.search;

import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.Query;
import sjtu.ipads.wtune.symsolver.core.TableSym;
import sjtu.ipads.wtune.symsolver.search.impl.SearchCtxImpl;
import sjtu.ipads.wtune.symsolver.smt.Proposition;
import sjtu.ipads.wtune.symsolver.smt.SmtCtx;

import java.util.List;
import java.util.Map;

public interface SearchCtx extends Tracer, Prover {
  List<Summary> search(DecisionTree trees);

  void record();

  static SearchCtx make(TableSym[] tables, PickSym[] picks, SmtCtx smtCtx, Query q0, Query q1) {
    return SearchCtxImpl.build(tables, picks, smtCtx, q0, q1);
  }
}
