package sjtu.ipads.wtune.symsolver.search;

import sjtu.ipads.wtune.symsolver.core.*;
import sjtu.ipads.wtune.symsolver.logic.LogicCtx;
import sjtu.ipads.wtune.symsolver.search.impl.SearchCtxImpl;

import java.util.List;

public interface SearchCtx extends Tracer, Prover {
  List<Summary> search(DecisionTree trees);

  void record();

  static SearchCtx make(
      TableSym[] tables, PickSym[] picks, PredicateSym[] preds, LogicCtx ctx, Query q0, Query q1) {
    return SearchCtxImpl.build(tables, picks, preds, ctx, q0, q1);
  }

  static SearchCtx make(
      TableSym[] tables,
      PickSym[] picks,
      PredicateSym[] preds,
      LogicCtx ctx,
      Query q0,
      Query q1,
      long timeout) {
    return SearchCtxImpl.build(tables, picks, preds, ctx, q0, q1, timeout);
  }
}
