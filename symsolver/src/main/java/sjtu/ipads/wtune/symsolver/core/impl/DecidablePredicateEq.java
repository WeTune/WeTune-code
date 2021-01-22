package sjtu.ipads.wtune.symsolver.core.impl;

import sjtu.ipads.wtune.symsolver.DecidableConstraint;
import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.Indexed;
import sjtu.ipads.wtune.symsolver.core.PredicateSym;
import sjtu.ipads.wtune.symsolver.search.Reactor;

import java.util.function.Function;

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
  @SuppressWarnings("unchecked")
  public <T, R extends Indexed> Constraint unwrap(Function<T, R> func) {
    return BasePredicateEq.build(
        func.apply((T) px().unwrap(Object.class)), func.apply((T) py().unwrap(Object.class)));
  }

  @Override
  public void decide(Reactor reactor) {
    reactor.predicateEq(this, px(), py());
  }
}
