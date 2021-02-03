package sjtu.ipads.wtune.symsolver.core.impl;

import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.Reference;

import java.util.Objects;

public class BaseReference<T, P> implements Reference<T, P> {
  private final T tx, ty;
  private final P px, py;

  protected BaseReference(T tx, P px, T ty, P py) {
    this.tx = tx;
    this.ty = ty;
    this.px = px;
    this.py = py;
  }

  public static <T, P> Constraint build(T tx, P px, T ty, P py) {
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
  public Object[] targets() {
    return new Object[] {tx(), px(), ty(), py()};
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
