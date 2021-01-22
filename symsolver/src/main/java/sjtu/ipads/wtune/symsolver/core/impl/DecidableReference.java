package sjtu.ipads.wtune.symsolver.core.impl;

import sjtu.ipads.wtune.symsolver.DecidableConstraint;
import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.Indexed;
import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.TableSym;
import sjtu.ipads.wtune.symsolver.search.Reactor;

import java.util.function.Function;

public class DecidableReference extends BaseReference<TableSym, PickSym>
    implements DecidableConstraint {

  private DecidableReference(TableSym tx, PickSym px, TableSym ty, PickSym py) {
    super(tx, px, ty, py);
  }

  public static DecidableConstraint build(TableSym tx, PickSym px, TableSym ty, PickSym py) {
    checkIndex(tx, px, ty, py);
    return new DecidableReference(tx, px, ty, py);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T, R extends Indexed> Constraint unwrap(Function<T, R> func) {
    return BaseReference.build(
        func.apply((T) tx().unwrap(Object.class)),
        func.apply((T) px().unwrap(Object.class)),
        func.apply((T) ty().unwrap(Object.class)),
        func.apply((T) py().unwrap(Object.class)));
  }

  @Override
  public void decide(Reactor reactor) {
    reactor.reference(this, tx(), px(), ty(), py());
  }
}
