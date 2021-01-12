package sjtu.ipads.wtune.symsolver.core.impl;

import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.Indexed;
import sjtu.ipads.wtune.symsolver.core.PickEq;

import java.util.Objects;

public class BasePickEq<P extends Indexed> implements PickEq<P> {
  private final P px, py;

  protected BasePickEq(P x, P y) {
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
  public Indexed[] targets() {
    return new Indexed[] {px(), py()};
  }

  @Override
  public int compareTo(Constraint o) {
    int res = kind().compareTo(o.kind());
    if (res != 0) return res;

    final PickEq<?> other = (PickEq<?>) o;
    res = px().compareTo(other.px());
    return res != 0 ? res : py().compareTo(other.py());
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
