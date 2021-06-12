package sjtu.ipads.wtune.prover.expr;

import com.google.common.base.Objects;

class TupleImpl implements Tuple {
  private final Tuple base;
  private final Name name;

  TupleImpl(Tuple base, Name name) {
    this.base = base;
    this.name = name;
  }

  @Override
  public Tuple base() {
    return base;
  }

  @Override
  public Name name() {
    return name;
  }

  @Override
  public Tuple subst(Tuple target, Tuple replacement) {
    if (this.equals(target)) return replacement;
    if (base != null) return base.subst(target, replacement);
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Tuple)) return false;
    final Tuple tuple = (Tuple) o;
    return Objects.equal(name, tuple.name()) && Objects.equal(base, tuple.base());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(base, name);
  }

  @Override
  public String toString() {
    if (base == null) return name.toString();
    else return base + "." + name;
  }
}
