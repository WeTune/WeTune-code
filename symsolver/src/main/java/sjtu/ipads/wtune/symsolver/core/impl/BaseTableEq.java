package sjtu.ipads.wtune.symsolver.core.impl;

import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.Indexed;
import sjtu.ipads.wtune.symsolver.core.TableEq;

import java.util.Objects;

public class BaseTableEq<T extends Indexed> implements TableEq<T> {
  private final T tx, ty;

  protected BaseTableEq(T x, T y) {
    if (x.index() < y.index()) {
      tx = x;
      ty = y;
    } else {
      tx = y;
      ty = x;
    }
  }

  protected static void checkIndex(Indexed x, Indexed y) {
    if (x == null
        || y == null
        || x == y
        || !x.isIndexed()
        || !y.isIndexed()
        || x.index() == y.index()) throw new IllegalArgumentException();
  }

  public static <T extends Indexed> TableEq<T> build(T x, T y) {
    checkIndex(x, y);
    return new BaseTableEq<>(x, y);
  }

  @Override
  public Constraint.Kind kind() {
    return Constraint.Kind.TableEq;
  }

  @Override
  public T tx() {
    return tx;
  }

  @Override
  public T ty() {
    return ty;
  }

  @Override
  public Indexed[] targets() {
    return new Indexed[] {tx(), ty()};
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof TableEq)) return false;

    final TableEq<?> tableEq = (TableEq<?>) o;

    return (Objects.equals(tx(), tableEq.tx()) && Objects.equals(ty(), tableEq.ty()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(tx(), ty());
  }

  @Override
  public String toString() {
    return "TableEq(" + tx() + "," + ty() + ")";
  }

  @Override
  public int compareTo(Constraint o) {
    int res = kind().compareTo(o.kind());
    if (res != 0) return res;

    final TableEq<?> other = (TableEq<?>) o;
    res = tx().compareTo(other.tx());
    return res != 0 ? res : ty().compareTo(other.ty());
  }
}
