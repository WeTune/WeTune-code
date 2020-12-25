package sjtu.ipads.wtune.symsolver.core;

import sjtu.ipads.wtune.symsolver.core.impl.TracerImpl;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface Tracer extends Reactor {
  boolean isConflict();

  boolean isIncomplete();

  Summary summary();

  static ResettableTracer resettable(TableSym[] tables, PickSym[] picks) {
    return TracerImpl.build(tables, picks);
  }

  interface Summary {
    Collection<Constraint> constraints();

    Collection<Collection<TableSym>> eqTables();

    Collection<Collection<PickSym>> eqPicks();

    Collection<TableSym>[] srcs();

    Map<PickSym, PickSym> refs();

    boolean implies(Summary other);
  }
}
