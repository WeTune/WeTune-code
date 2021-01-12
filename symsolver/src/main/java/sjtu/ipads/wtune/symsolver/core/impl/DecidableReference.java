package sjtu.ipads.wtune.symsolver.core.impl;

import sjtu.ipads.wtune.symsolver.DecidableConstraint;
import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.Indexed;
import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.TableSym;
import sjtu.ipads.wtune.symsolver.search.Reactor;

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
  public <T extends Indexed> Constraint unwrap(Class<T> cls) {
    return BaseReference.build(
        tx().unwrap(cls), px().unwrap(cls), ty().unwrap(cls), py().unwrap(cls));
  }

  @Override
  public void decide(Reactor reactor) {
    reactor.reference(this, tx(), px(), ty(), py());
  }
}
