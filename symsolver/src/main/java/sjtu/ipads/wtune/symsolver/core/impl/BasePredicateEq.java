package sjtu.ipads.wtune.symsolver.core.impl;

import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.Indexed;
import sjtu.ipads.wtune.symsolver.core.PredicateEq;

import java.util.Objects;

public class BasePredicateEq<P extends Indexed> implements PredicateEq<P> {
  private final P px, py;

  protected BasePredicateEq(P x, P y) {
    if (x.index() < y.index()) {
      px = x;
      py = y;
    } else {
      px = y;
      py = x;
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

  public static <P extends Indexed> Constraint build(P x, P y) {
    checkIndex(x, y);
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
  public Indexed[] targets() {
    return new Indexed[] {px(), py()};
  }

  @Override
  public int compareTo(Constraint o) {
    int res = kind().compareTo(o.kind());
    if (res != 0) return res;

    final PredicateEq<?> other = (PredicateEq<?>) o;
    res = px().compareTo(other.px());
    return res != 0 ? res : py().compareTo(other.py());
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
