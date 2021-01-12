package sjtu.ipads.wtune.symsolver.core.impl;

import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.Indexed;
import sjtu.ipads.wtune.symsolver.core.Reference;

import java.util.Objects;

public class BaseReference<T extends Indexed, P extends Indexed> implements Reference<T, P> {
  private final T tx, ty;
  private final P px, py;

  protected BaseReference(T tx, P px, T ty, P py) {
    this.tx = tx;
    this.ty = ty;
    this.px = px;
    this.py = py;
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

  public static <T extends Indexed, P extends Indexed> Constraint build(T tx, P px, T ty, P py) {
    checkIndex(tx, px, ty, py);
    return new BaseReference<>(tx, px, ty, py);
  }

  @Override
  public Kind kind() {
    return Kind.Reference;
  }

  @Override
  public T tx() {
    return tx;
  }

  @Override
  public P px() {
    return px;
  }

  @Override
  public T ty() {
    return ty;
  }

  @Override
  public P py() {
    return py;
  }

  @Override
  public Indexed[] targets() {
    return new Indexed[] {tx(), px(), ty(), py()};
  }

  @Override
  public int compareTo(Constraint o) {
    int res = kind().compareTo(o.kind());
    if (res != 0) return res;

    final Reference<?, ?> other = (Reference<?, ?>) o;
    res = tx().compareTo(other.tx());
    res = res != 0 ? res : px().compareTo(other.px());
    res = res != 0 ? res : ty().compareTo(other.ty());
    res = res != 0 ? res : py().compareTo(other.py());
    return res;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final Reference<?, ?> reference = (Reference<?, ?>) o;
    return Objects.equals(tx(), reference.tx())
        && Objects.equals(ty(), reference.ty())
        && Objects.equals(px(), reference.px())
        && Objects.equals(py(), reference.py());
  }

  @Override
  public int hashCode() {
    return Objects.hash(tx(), ty(), px(), py());
  }

  @Override
  public String toString() {
    return "Reference(" + tx() + "," + px() + "," + ty() + "," + py() + ")";
  }
}
