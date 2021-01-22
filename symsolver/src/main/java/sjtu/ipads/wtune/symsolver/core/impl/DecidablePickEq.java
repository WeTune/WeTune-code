package sjtu.ipads.wtune.symsolver.core.impl;

import sjtu.ipads.wtune.symsolver.DecidableConstraint;
import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.Indexed;
import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.search.Reactor;

import java.util.function.Function;

public class DecidablePickEq extends BasePickEq<PickSym> implements DecidableConstraint {
  private DecidablePickEq(PickSym x, PickSym y) {
    super(x, y);
  }

  public static DecidableConstraint build(PickSym x, PickSym y) {
    checkIndex(x, y);
    return new DecidablePickEq(x, y);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T, R extends Indexed> Constraint unwrap(Function<T, R> func) {
    return BasePickEq.build(
        func.apply((T) px().unwrap(Object.class)), func.apply((T) py().unwrap(Object.class)));
  }

  @Override
  public void decide(Reactor reactor) {
    reactor.pickEq(this, px(), py());
  }

  @Override
  public boolean impossible() {
    return px().joined() == py() || py().joined() == px();
  }
}
