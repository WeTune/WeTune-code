package sjtu.ipads.wtune.symsolver.search;

import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.TableSym;
import sjtu.ipads.wtune.symsolver.search.impl.TracerImpl;

public interface Tracer extends Reactor {
  static Tracer bindTo(TableSym[] tables, PickSym[] picks) {
    return TracerImpl.build(tables, picks);
  }

  boolean isConflict();

  boolean isIncomplete();

  Summary summary();
}
