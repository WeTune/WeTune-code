package sjtu.ipads.wtune.symsolver.core;

import sjtu.ipads.wtune.symsolver.logic.LogicCtx;

public interface QueryBuilder {
  Query build(LogicCtx ctx, int tableIdxStart, int pickIdxStart, int predIdxStart);
}
