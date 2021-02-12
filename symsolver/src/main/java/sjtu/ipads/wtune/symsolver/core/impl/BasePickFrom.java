package sjtu.ipads.wtune.symsolver.core.impl;

import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.PickFrom;

import java.util.Arrays;
import java.util.Objects;

public class BasePickFrom<T, P> implements PickFrom<T, P> {
  private final P p;
  private final T[] ts;

  protected BasePickFrom(P p, T[] ts) {
    this.p = p;
    this.ts = ts;
  }

  public static <T, P> Constraint build(P p, T[] ts) {
    return new BasePickFrom<>(p, ts);
  }

  @Override
  public Kind kind() {
    return Kind.PickFrom;
  }

  @Override
  public P p() {
    return p;
  }

  @Override
  public T[] ts() {
    return ts;
  }

  @Override
  public Object[] targets() {
    final Object[] syms = new Object[1 + ts.length];
    syms[0] = p();

    System.arraycopy(ts(), 0, syms, 1, ts.length);
    return syms;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final PickFrom<?, ?> pickFrom = (PickFrom<?, ?>) o;
    return Objects.equals(p(), pickFrom.p()) && Arrays.equals(ts(), pickFrom.ts());
  }

  @Override
  public int hashCode() {
    return Objects.hash(p()) + Arrays.hashCode(ts());
  }

  @Override
  public String toString() {
    return "PickFrom(" + p() + "," + Arrays.toString(ts()) + ")";
  }
}
