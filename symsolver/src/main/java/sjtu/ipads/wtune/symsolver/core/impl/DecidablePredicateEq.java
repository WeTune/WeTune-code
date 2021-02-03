package sjtu.ipads.wtune.symsolver.core.impl;

import sjtu.ipads.wtune.symsolver.DecidableConstraint;
import sjtu.ipads.wtune.symsolver.core.Indexed;
import sjtu.ipads.wtune.symsolver.core.PredicateSym;
import sjtu.ipads.wtune.symsolver.search.Reactor;

public class DecidablePredicateEq extends BasePredicateEq<PredicateSym>
    implements DecidableConstraint {
  private DecidablePredicateEq(PredicateSym x, PredicateSym y) {
    super(x, y);
  }

  protected static void checkIndex(Indexed x, Indexed y) {
    if (x == null
        || y == null
        || x == y
        || !x.isIndexed()
        || !y.isIndexed()
        || x.index() == y.index()) throw new IllegalArgumentException();
  }

  public static DecidablePredicateEq build(PredicateSym x, PredicateSym y) {
    checkIndex(x, y);
    return x.index() < y.index() ? new DecidablePredicateEq(x, y) : new DecidablePredicateEq(y, x);
  }

  @Override
  public void decide(Reactor reactor) {
    reactor.predicateEq(this, px(), py());
  }

  @Override
  public int compareTo(DecidableConstraint o) {
    int res = kind().compareTo(o.kind());
    if (res != 0) return res;

    final DecidablePredicateEq other = (DecidablePredicateEq) o;
    res = px().compareTo(other.px());
    return res != 0 ? res : py().compareTo(other.py());
  }
}
