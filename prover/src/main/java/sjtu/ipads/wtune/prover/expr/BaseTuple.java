package sjtu.ipads.wtune.prover.expr;

import static java.util.Objects.requireNonNull;

final class BaseTuple implements Tuple {
  private final Name name;

  BaseTuple(Name name) {
    this.name = requireNonNull(name);
  }

  public Tuple[] base() {
    return null;
  }

  public Name name() {
    return name;
  }

  public Tuple subst(Tuple t, Tuple rep) {
    requireNonNull(t);
    requireNonNull(rep);
    if (this.equals(t)) return rep;
    else return this;
  }

  @Override
  public Tuple root() {
    return this;
  }

  @Override
  public String toString() {
    return name.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof BaseTuple)) return false;
    final BaseTuple that = (BaseTuple) o;
    return this.name.equals(that.name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }
}
