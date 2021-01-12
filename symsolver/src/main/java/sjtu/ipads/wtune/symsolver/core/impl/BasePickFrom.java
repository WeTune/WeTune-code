package sjtu.ipads.wtune.symsolver.core.impl;

import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.Indexed;
import sjtu.ipads.wtune.symsolver.core.PickFrom;
import sjtu.ipads.wtune.symsolver.core.Sym;

import java.util.Arrays;
import java.util.Objects;

import static java.util.function.Predicate.not;
import static sjtu.ipads.wtune.common.utils.FuncUtils.sorted;
import static sjtu.ipads.wtune.common.utils.FuncUtils.stream;

public class BasePickFrom<T extends Indexed, P extends Indexed> implements PickFrom<T, P> {
  private final P p;
  private final T[] ts;

  protected BasePickFrom(P p, T[] ts) {
    this.p = p;
    this.ts = sorted(ts, Indexed::compareTo);
  }

  protected static void checkIndex(Indexed p, Indexed[] ts) {
    if (p == null || ts == null || !p.isIndexed() || stream(ts).anyMatch(not(Indexed::isIndexed)))
      throw new IllegalArgumentException();
  }

  public static <T extends Indexed, P extends Indexed> Constraint build(P p, T[] ts) {
    checkIndex(p, ts);
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
  public Indexed[] targets() {
    final Indexed[] syms = new Sym[1 + ts.length];
    syms[0] = p();

    System.arraycopy(ts(), 0, syms, 1, ts.length);
    return syms;
  }

  @Override
  public int compareTo(Constraint o) {
    int res = kind().compareTo(o.kind());
    if (res != 0) return res;

    final PickFrom<?, ?> other = (PickFrom<?, ?>) o;
    res = p().compareTo(other.p());
    return res != 0 ? res : Arrays.compare(ts(), other.ts());
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
