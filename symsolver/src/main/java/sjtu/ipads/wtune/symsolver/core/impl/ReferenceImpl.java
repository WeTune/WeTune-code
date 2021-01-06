package sjtu.ipads.wtune.symsolver.core.impl;

import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.Sym;
import sjtu.ipads.wtune.symsolver.core.TableSym;
import sjtu.ipads.wtune.symsolver.search.Reactor;
import sjtu.ipads.wtune.symsolver.utils.Indexed;

import java.util.Objects;

public class ReferenceImpl implements Constraint {
  private final TableSym tx, ty;
  private final PickSym px, py;

  private ReferenceImpl(TableSym tx, PickSym px, TableSym ty, PickSym py) {
    this.tx = tx;
    this.ty = ty;
    this.px = px;
    this.py = py;
  }

  public static Constraint build(TableSym tx, PickSym px, TableSym ty, PickSym py) {
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

    return new ReferenceImpl(tx, px, ty, py);
  }

  @Override
  public Kind kind() {
    return Kind.Reference;
  }

  @Override
  public void decide(Reactor reactor) {
    reactor.reference(this, tx, px, ty, py);
  }

  @Override
  public Sym[] targets() {
    return new Sym[] {tx, px, ty, py};
  }

  @Override
  public int compareTo(Constraint o) {
    int res = kind().compareTo(o.kind());
    if (res != 0) return res;

    final ReferenceImpl other = (ReferenceImpl) o;
    res = tx.compareTo(other.tx);
    res = res != 0 ? res : px.compareTo(other.px);
    res = res != 0 ? res : ty.compareTo(other.ty);
    res = res != 0 ? res : py.compareTo(other.py);
    return res;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ReferenceImpl reference = (ReferenceImpl) o;
    return Objects.equals(tx, reference.tx)
        && Objects.equals(ty, reference.ty)
        && Objects.equals(px, reference.px)
        && Objects.equals(py, reference.py);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tx, ty, px, py);
  }

  @Override
  public String toString() {
    return "Reference(" + tx + "," + px + "," + ty + "," + py + ")";
  }
}
