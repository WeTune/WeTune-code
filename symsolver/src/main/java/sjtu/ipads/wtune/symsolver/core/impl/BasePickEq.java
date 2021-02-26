package sjtu.ipads.wtune.symsolver.core.impl;

import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.PickEq;

import java.util.Objects;

public class BasePickEq<P> implements PickEq<P> {
  private final P px, py;

  protected BasePickEq(P x, P y) {
    px = x;
    py = y;
  }

  public static <P> Constraint build(P x, P y) {
    return new BasePickEq<>(x, y);
  }

  @Override
  public Constraint.Kind kind() {
    return Constraint.Kind.PickEq;
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
  public Constraint flip() {
    return new BasePickEq<>(py, px);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final PickEq<?> pickEq = (PickEq<?>) o;

    return (Objects.equals(px(), pickEq.px()) && Objects.equals(py(), pickEq.py()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(px(), py());
  }

  @Override
  public String toString() {
    return "PickEq(" + px() + "," + py() + ")";
  }
}
