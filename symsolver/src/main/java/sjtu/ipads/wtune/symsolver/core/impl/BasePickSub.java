package sjtu.ipads.wtune.symsolver.core.impl;

import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.PickSub;

import java.util.Objects;

public class BasePickSub<P> implements PickSub<P> {
  private final P px, py;

  public BasePickSub(P px, P py) {
    this.px = px;
    this.py = py;
  }

  public static <P> BasePickSub<P> build(P px, P py) {
    return new BasePickSub<>(px, py);
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
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BasePickSub<?> that = (BasePickSub<?>) o;
    return Objects.equals(px, that.px) && Objects.equals(py, that.py);
  }

  @Override
  public int hashCode() {
    return Objects.hash(px, py);
  }

  @Override
  public String toString() {
    return "PickSub(" + px + ',' + py + ')';
  }
}
