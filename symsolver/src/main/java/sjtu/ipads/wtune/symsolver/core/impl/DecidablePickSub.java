package sjtu.ipads.wtune.symsolver.core.impl;

import sjtu.ipads.wtune.symsolver.DecidableConstraint;
import sjtu.ipads.wtune.symsolver.core.Indexed;
import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.search.Reactor;

public class DecidablePickSub extends BasePickSub<PickSym> implements DecidableConstraint {
  public DecidablePickSub(PickSym px, PickSym py) {
    super(px, py);
  }

  protected static void checkIndex(Indexed x, Indexed y) {
    if (x == null
        || y == null
        || x == y
        || !x.isIndexed()
        || !y.isIndexed()
        || x.index() == y.index()) throw new IllegalArgumentException();
  }

  public static DecidableConstraint build(PickSym px, PickSym py) {
    checkIndex(px, py);
    //    if (px.upstream() != py) throw new IllegalArgumentException();
    return new DecidablePickSub(px, py);
  }

  @Override
  public int compareTo(DecidableConstraint o) {
    int res = kind().compareTo(o.kind());
    if (res != 0) return res;

    final DecidablePickSub other = (DecidablePickSub) o;
    res = px().compareTo(other.px());
    return res != 0 ? res : py().compareTo(other.py());
  }

  @Override
  public void decide(Reactor reactor) {
    reactor.pickSub(this, px(), py());
  }
}
