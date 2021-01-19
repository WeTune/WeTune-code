package sjtu.ipads.wtune.symsolver.core;

import sjtu.ipads.wtune.symsolver.logic.LogicCtx;

public interface QueryBuilder {
  LogicCtx ctx();

  TableSym tableSym(Scoped owner);

  PickSym pickSym(Scoped owner);

  PredicateSym predSym(Scoped owner);

  Query build(LogicCtx ctx, int tableIdxStart, int pickIdxStart, int predIdxStart);
}
