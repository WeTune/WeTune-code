package sjtu.ipads.wtune.symsolver.core.impl;

import sjtu.ipads.wtune.symsolver.DecidableConstraint;
import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.Indexed;
import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.search.Reactor;

public class DecidablePickEq extends BasePickEq<PickSym> implements DecidableConstraint {
  private DecidablePickEq(PickSym x, PickSym y) {
    super(x, y);
  }

  public static DecidableConstraint build(PickSym x, PickSym y) {
    checkIndex(x, y);
    return new DecidablePickEq(x, y);
  }

  @Override
  public <T extends Indexed> Constraint unwrap(Class<T> cls) {
    return BasePickEq.build(px().unwrap(cls), py().unwrap(cls));
  }

  @Override
  public void decide(Reactor reactor) {
    reactor.pickEq(this, px(), py());
  }
}
