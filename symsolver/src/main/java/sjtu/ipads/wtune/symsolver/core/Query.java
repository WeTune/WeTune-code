package sjtu.ipads.wtune.symsolver.core;

import sjtu.ipads.wtune.symsolver.logic.LogicCtx;
import sjtu.ipads.wtune.symsolver.logic.Proposition;
import sjtu.ipads.wtune.symsolver.logic.Value;

public interface Query {
  TableSym[] tables();

  PickSym[] picks();

  PredicateSym[] preds();

  Value[] tuples();

  Value[] output();

  Proposition condition();

  // for test
  static Query buildFrom(QueryBuilder builder) {
    return builder.build(LogicCtx.z3(), "x", 0, 0, 0);
  }
}
