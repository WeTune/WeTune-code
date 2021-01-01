package sjtu.ipads.wtune.symsolver.search;

import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.TableSym;

import java.util.Collection;
import java.util.Map;

public interface Summary {
  Constraint[] constraints();

  Collection<Collection<TableSym>> tableGroups();

  Collection<Collection<PickSym>> pickGroups();

  TableSym[][] pivotedSources();

  Map<PickSym, PickSym> references();

  boolean implies(Summary other);
}
