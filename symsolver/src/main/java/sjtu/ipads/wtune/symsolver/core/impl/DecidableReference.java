package sjtu.ipads.wtune.symsolver.core.impl;

import sjtu.ipads.wtune.symsolver.DecidableConstraint;
import sjtu.ipads.wtune.symsolver.core.Indexed;
import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.TableSym;
import sjtu.ipads.wtune.symsolver.search.Reactor;

public class DecidableReference extends BaseReference<TableSym, PickSym>
    implements DecidableConstraint {

  private DecidableReference(TableSym tx, PickSym px, TableSym ty, PickSym py) {
    super(tx, px, ty, py);
  }

  protected static void checkIndex(Indexed tx, Indexed px, Indexed ty, Indexed py) {
    if (tx == null
        || ty == null
        || px == null
        || py == null
        || tx == ty
        || px == py
        || !tx.isIndexed()
        || !px.isIndexed()
        || !ty.isIndexed()
        || !py.isIndexed()) throw new IllegalArgumentException();
  }

  public static DecidableConstraint build(TableSym tx, PickSym px, TableSym ty, PickSym py) {
    checkIndex(tx, px, ty, py);
    return new DecidableReference(tx, px, ty, py);
  }

  @Override
  public void decide(Reactor reactor) {
    reactor.reference(this, tx(), px(), ty(), py());
  }

  @Override
  public int compareTo(DecidableConstraint o) {
    int res = kind().compareTo(o.kind());
    if (res != 0) return res;

    final DecidableReference other = (DecidableReference) o;
    res = tx().compareTo(other.tx());
    res = res != 0 ? res : px().compareTo(other.px());
    res = res != 0 ? res : ty().compareTo(other.ty());
    res = res != 0 ? res : py().compareTo(other.py());
    return res;
  }
}
