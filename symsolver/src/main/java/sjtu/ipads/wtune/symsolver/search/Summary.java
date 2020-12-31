package sjtu.ipads.wtune.symsolver.search;

import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.TableSym;

import java.util.Collection;
import java.util.Map;

public interface Summary {
  Constraint[] constraints();

  Collection<Collection<TableSym>> eqTables();

  Collection<Collection<PickSym>> eqPicks();

  TableSym[][] srcs();

  Map<PickSym, PickSym> refs();

  boolean implies(Summary other);
}
