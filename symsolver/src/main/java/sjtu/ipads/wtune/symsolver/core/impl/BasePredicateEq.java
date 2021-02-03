package sjtu.ipads.wtune.symsolver.core.impl;

import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.PredicateEq;

import java.util.Objects;

public class BasePredicateEq<P> implements PredicateEq<P> {
  private final P px, py;

  protected BasePredicateEq(P x, P y) {
    px = x;
    py = y;
  }

  public static <P> Constraint build(P x, P y) {
    return new BasePredicateEq<>(x, y);
  }

  @Override
  public Constraint.Kind kind() {
    return Kind.PredicateEq;
  }

  @Override
  public P px() {
    return px;
  }

  @Override
  public P py() {
    return py;
  }

  @Override
  public Object[] targets() {
    return new Object[] {px(), py()};
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final PredicateEq<?> other = (PredicateEq<?>) o;

    return (Objects.equals(px(), other.px()) && Objects.equals(py(), other.py()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(px(), py());
  }

  @Override
  public String toString() {
    return "PredicateEq(" + px() + "," + py() + ")";
  }
}
