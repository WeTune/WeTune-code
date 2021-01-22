package sjtu.ipads.wtune.symsolver.core.impl;

import sjtu.ipads.wtune.symsolver.DecidableConstraint;
import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.Indexed;
import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.TableSym;
import sjtu.ipads.wtune.symsolver.search.Reactor;

import java.util.function.Function;

import static sjtu.ipads.wtune.common.utils.FuncUtils.arrayMap;

public class DecidablePickFrom extends BasePickFrom<TableSym, PickSym>
    implements DecidableConstraint {

  private DecidablePickFrom(PickSym p, TableSym[] ts) {
    super(p, ts);
  }

  public static DecidableConstraint build(PickSym p, TableSym[] ts) {
    checkIndex(p, ts);

    return new DecidablePickFrom(p, ts);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T, R extends Indexed> Constraint unwrap(Function<T, R> func) {
    return BasePickFrom.build(
        func.apply((T) p().unwrap(Object.class)),
        arrayMap(it -> func.apply((T) it.unwrap(Object.class)), Indexed.class, ts()));
  }

  @Override
  public void decide(Reactor reactor) {
    reactor.pickFrom(this, p(), ts());
  }

  @Override
  public boolean ignorable() {
    return ts().length == p().visibleSources().length;
  }
}
