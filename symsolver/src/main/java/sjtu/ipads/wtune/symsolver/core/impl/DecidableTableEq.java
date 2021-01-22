package sjtu.ipads.wtune.symsolver.core.impl;

import sjtu.ipads.wtune.symsolver.DecidableConstraint;
import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.Indexed;
import sjtu.ipads.wtune.symsolver.core.TableSym;
import sjtu.ipads.wtune.symsolver.search.Reactor;

import java.util.function.Function;

public class DecidableTableEq extends BaseTableEq<TableSym> implements DecidableConstraint {
  private DecidableTableEq(TableSym x, TableSym y) {
    super(x, y);
  }

  public static DecidableConstraint build(TableSym x, TableSym y) {
    checkIndex(x, y);
    return new DecidableTableEq(x, y);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T, R extends Indexed> Constraint unwrap(Function<T, R> func) {
    return BaseTableEq.build(
        func.apply((T) tx().unwrap(Object.class)), func.apply((T) ty().unwrap(Object.class)));
  }

  @Override
  public void decide(Reactor reactor) {
    reactor.tableEq(this, tx(), ty());
  }
}
