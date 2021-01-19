package sjtu.ipads.wtune.symsolver.core;

import sjtu.ipads.wtune.symsolver.logic.LogicCtx;
import sjtu.ipads.wtune.symsolver.logic.Proposition;
import sjtu.ipads.wtune.symsolver.logic.Value;

public interface Query {
  TableSym[] tables();

  PickSym[] picks();

  PredicateSym[] preds();

  Proposition contains(Value v);

  // for test
  static Query buildFrom(QueryBuilder builder) {
    return builder.build(LogicCtx.z3(), 0, 0, 0);
  }
}
