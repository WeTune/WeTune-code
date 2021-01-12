package sjtu.ipads.wtune.symsolver.core.impl;

import sjtu.ipads.wtune.symsolver.DecidableConstraint;
import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.Indexed;
import sjtu.ipads.wtune.symsolver.core.PredicateSym;
import sjtu.ipads.wtune.symsolver.search.Reactor;

public class DecidablePredicateEq extends BasePredicateEq<PredicateSym>
    implements DecidableConstraint {
  private DecidablePredicateEq(PredicateSym x, PredicateSym y) {
    super(x, y);
  }

  public static DecidablePredicateEq build(PredicateSym x, PredicateSym y) {
    checkIndex(x, y);
    return new DecidablePredicateEq(x, y);
  }

  @Override
  public <T extends Indexed> Constraint unwrap(Class<T> cls) {
    return BasePredicateEq.build(px().unwrap(cls), py().unwrap(cls));
  }

  @Override
  public void decide(Reactor reactor) {
    reactor.predicateEq(this, px(), py());
  }
}
