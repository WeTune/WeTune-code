package sjtu.ipads.wtune.symsolver.core.impl;

import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.Sym;
import sjtu.ipads.wtune.symsolver.core.TableSym;
import sjtu.ipads.wtune.symsolver.search.Reactor;
import sjtu.ipads.wtune.symsolver.utils.Indexed;

import java.util.Comparator;
import java.util.Objects;

public class TableEqImpl implements Constraint {
  private final TableSym tx, ty;

  private TableEqImpl(TableSym x, TableSym y) {
    if (x.index() < y.index()) {
      tx = x;
      ty = y;
    } else {
      tx = y;
      ty = x;
    }
  }

  public static Constraint build(TableSym x, TableSym y) {
    if (x == null
        || y == null
        || x == y
        || x.index() == Indexed.UNKNOWN_INDEX
        || y.index() == Indexed.UNKNOWN_INDEX
        || x.index() == y.index()) throw new IllegalArgumentException();
    return new TableEqImpl(x, y);
  }

  @Override
  public Constraint.Kind kind() {
    return Constraint.Kind.TableEq;
  }

  @Override
  public void decide(Reactor reactor) {
    reactor.tableEq(this, tx, ty);
  }

  @Override
  public Sym[] targets() {
    return new Sym[] {tx, ty};
  }

  @Override
  public int compareTo(Constraint o) {
    int res = kind().compareTo(o.kind());
    if (res != 0) return res;

    final TableEqImpl other = (TableEqImpl) o;
    res = tx.compareTo(other.tx);
    return res != 0 ? res : ty.compareTo(other.ty);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final TableEqImpl tableEq = (TableEqImpl) o;

    return (Objects.equals(tx, tableEq.tx) && Objects.equals(ty, tableEq.ty));
  }

  @Override
  public int hashCode() {
    return Objects.hash(tx, ty);
  }

  @Override
  public String toString() {
    return "TableEq(" + tx + "," + ty + ")";
  }
}
