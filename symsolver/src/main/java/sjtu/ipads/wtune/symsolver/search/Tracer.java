package sjtu.ipads.wtune.symsolver.search;

import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.PredicateSym;
import sjtu.ipads.wtune.symsolver.core.Summary;
import sjtu.ipads.wtune.symsolver.core.TableSym;
import sjtu.ipads.wtune.symsolver.search.impl.TracerImpl;

public interface Tracer extends Reactor {
  static Tracer bindTo(TableSym[] tables, PickSym[] picks, PredicateSym[] preds) {
    return TracerImpl.build(tables, picks, preds);
  }

  boolean isConflict();

  boolean isIncomplete();

  Summary summary();

  int numFastConflict();

  int numFastIncomplete();
}
