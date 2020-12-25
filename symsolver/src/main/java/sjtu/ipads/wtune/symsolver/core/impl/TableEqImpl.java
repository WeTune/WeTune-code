package sjtu.ipads.wtune.symsolver.core.impl;

import sjtu.ipads.wtune.symsolver.core.Reactor;
import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.TableSym;

import java.util.Objects;

public class TableEqImpl implements Constraint {
  private final TableSym tx, ty;

  private TableEqImpl(TableSym x, TableSym y) {
    tx = x;
    ty = y;
  }

  public static Constraint build(TableSym x, TableSym y) {
    if (x == null || y == null || x == y) throw new IllegalArgumentException();
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
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final TableEqImpl tableEq = (TableEqImpl) o;

    return (Objects.equals(tx, tableEq.tx) && Objects.equals(ty, tableEq.ty))
        || (Objects.equals(tx, tableEq.ty) && Objects.equals(ty, tableEq.tx));
  }

  @Override
  public int hashCode() {
    return Objects.hash(tx, ty) + Objects.hash(ty, tx);
  }

  @Override
  public String toString() {
    return "TableEq(" + tx + "," + ty + ")";
  }
}
