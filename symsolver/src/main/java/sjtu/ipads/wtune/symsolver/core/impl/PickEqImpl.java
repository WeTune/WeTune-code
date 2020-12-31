package sjtu.ipads.wtune.symsolver.core.impl;

import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.Sym;
import sjtu.ipads.wtune.symsolver.search.Reactor;
import sjtu.ipads.wtune.symsolver.utils.Indexed;

import java.util.Objects;

public class PickEqImpl implements Constraint {
  private final PickSym px, py;

  private PickEqImpl(PickSym x, PickSym y) {
    if (x.index() < y.index()) {
      px = x;
      py = y;
    } else {
      px = y;
      py = x;
    }
  }

  public static Constraint build(PickSym x, PickSym y) {
    if (x == null
        || y == null
        || x == y
        || x.index() == Indexed.UNKNOWN_INDEX
        || y.index() == Indexed.UNKNOWN_INDEX
        || x.index() == y.index()) throw new IllegalArgumentException();
    return new PickEqImpl(x, y);
  }

  @Override
  public Constraint.Kind kind() {
    return Constraint.Kind.PickEq;
  }

  @Override
  public void decide(Reactor reactor) {
    reactor.pickEq(this, px, py);
  }

  @Override
  public Sym[] targets() {
    return new Sym[] {px, py};
  }

  @Override
  public int compareTo(Constraint o) {
    int res = kind().compareTo(o.kind());
    if (res != 0) return res;

    final PickEqImpl other = (PickEqImpl) o;
    res = px.compareTo(other.px);
    return res != 0 ? res : py.compareTo(other.py);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final PickEqImpl pickEq = (PickEqImpl) o;

    return (Objects.equals(px, pickEq.px) && Objects.equals(py, pickEq.py))
        || (Objects.equals(px, pickEq.py) && Objects.equals(py, pickEq.px));
  }

  @Override
  public int hashCode() {
    return Objects.hash(px, py) + Objects.hash(py, px);
  }

  @Override
  public String toString() {
    return "PickEq(" + px + "," + py + ")";
  }
}
