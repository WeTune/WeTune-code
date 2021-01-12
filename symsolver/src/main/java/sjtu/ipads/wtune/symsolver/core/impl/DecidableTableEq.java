package sjtu.ipads.wtune.symsolver.core.impl;

import sjtu.ipads.wtune.symsolver.DecidableConstraint;
import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.TableSym;
import sjtu.ipads.wtune.symsolver.search.Reactor;

public class DecidableTableEq extends BaseTableEq<TableSym> implements DecidableConstraint {
  private DecidableTableEq(TableSym x, TableSym y) {
    super(x, y);
  }

  public static DecidableConstraint build(TableSym x, TableSym y) {
    checkIndex(x, y);
    return new DecidableTableEq(x, y);
  }

  @Override
  public void decide(Reactor reactor) {
    reactor.tableEq(this, tx(), ty());
  }
}
