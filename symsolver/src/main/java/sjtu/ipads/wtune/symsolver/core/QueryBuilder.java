package sjtu.ipads.wtune.symsolver.core;

import sjtu.ipads.wtune.symsolver.logic.LogicCtx;
import sjtu.ipads.wtune.symsolver.logic.Proposition;
import sjtu.ipads.wtune.symsolver.logic.Value;

public interface QueryBuilder {
  LogicCtx ctx();

  TableSym tableSym(Scoped owner);

  PickSym pickSym(Scoped owner);

  PredicateSym predSym(Scoped owner);

  int numTables();

  int numPicks();

  int numPreds();

  Value[] output();

  Proposition condition();

  Query build(LogicCtx ctx, String name, int tableIdxStart, int pickIdxStart, int predIdxStart);
}
