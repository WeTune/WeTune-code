package sjtu.ipads.wtune.symsolver.core;

import java.util.Collection;
import java.util.Map;

public interface Summary {
  Constraint[] constraints();

  Collection<Collection<TableSym>> tableGroups();

  Collection<Collection<PickSym>> pickGroups();

  Collection<Collection<PredicateSym>> predicateGroups();

  TableSym[][] pivotedSources();

  Map<PickSym, PickSym> references();

  boolean implies(Summary other);
}
