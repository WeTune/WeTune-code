package sjtu.ipads.wtune.symsolver.core.impl;

import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.TableEq;

import java.util.Objects;

public class BaseTableEq<T> implements TableEq<T> {
  private final T tx, ty;

  protected BaseTableEq(T x, T y) {
    tx = x;
    ty = y;
  }

  public static <T> TableEq<T> build(T x, T y) {
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
  public Object[] targets() {
    return new Object[] {tx(), ty()};
  }

  @Override
  public Constraint flip() {
    return new BaseTableEq<>(ty, tx);
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
}
