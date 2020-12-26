package sjtu.ipads.wtune.symsolver.core.impl;

import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.Sym;
import sjtu.ipads.wtune.symsolver.search.Reactor;

import java.util.Objects;

public class PickEqImpl implements Constraint {
  private final PickSym px, py;

  private PickEqImpl(PickSym x, PickSym y) {
    px = x;
    py = y;
  }

  public static Constraint build(PickSym x, PickSym y) {
    if (x == null || y == null || x == y) throw new IllegalArgumentException();
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
