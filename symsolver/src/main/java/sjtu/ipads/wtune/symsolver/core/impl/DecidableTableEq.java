package sjtu.ipads.wtune.symsolver.core.impl;

import sjtu.ipads.wtune.symsolver.DecidableConstraint;
import sjtu.ipads.wtune.symsolver.core.Indexed;
import sjtu.ipads.wtune.symsolver.core.TableSym;
import sjtu.ipads.wtune.symsolver.search.Reactor;

public class DecidableTableEq extends BaseTableEq<TableSym> implements DecidableConstraint {
  private DecidableTableEq(TableSym x, TableSym y) {
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

  public static DecidableConstraint build(TableSym x, TableSym y) {
    checkIndex(x, y);
    return x.index() < y.index() ? new DecidableTableEq(x, y) : new DecidableTableEq(y, x);
  }

  @Override
  public void decide(Reactor reactor) {
    reactor.tableEq(this, tx(), ty());
  }

  @Override
  public int compareTo(DecidableConstraint o) {
    int res = kind().compareTo(o.kind());
    if (res != 0) return res;

    final DecidableTableEq other = (DecidableTableEq) o;
    res = tx().compareTo(other.tx());
    return res != 0 ? res : ty().compareTo(other.ty());
  }
}
