package sjtu.ipads.wtune.symsolver.core.impl;

import sjtu.ipads.wtune.symsolver.DecidableConstraint;
import sjtu.ipads.wtune.symsolver.core.Indexed;
import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.search.Reactor;

public class DecidablePickEq extends BasePickEq<PickSym> implements DecidableConstraint {
  private DecidablePickEq(PickSym x, PickSym y) {
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

  public static DecidableConstraint build(PickSym x, PickSym y) {
    checkIndex(x, y);
    return x.index() < y.index() ? new DecidablePickEq(x, y) : new DecidablePickEq(y, x);
  }

  @Override
  public void decide(Reactor reactor) {
    reactor.pickEq(this, px(), py());
  }

  @Override
  public boolean impossible() {
    return px().joined() == py() || py().joined() == px();
  }

  @Override
  public int compareTo(DecidableConstraint o) {
    int res = kind().compareTo(o.kind());
    if (res != 0) return res;

    final DecidablePickEq other = (DecidablePickEq) o;
    res = px().compareTo(other.px());
    return res != 0 ? res : py().compareTo(other.py());
  }
}
