package sjtu.ipads.wtune.prover.uexpr;

import static java.util.Objects.requireNonNull;

final class BaseVar implements Var {
  private final Name name;

  BaseVar(Name name) {
    this.name = requireNonNull(name);
  }

  public Var[] base() {
    return null;
  }

  public Name name() {
    return name;
  }

  public Var subst(Var t, Var rep) {
    requireNonNull(t);
    requireNonNull(rep);
    if (this.equals(t)) return rep;
    else return this;
  }

  @Override
  public Var root() {
    return this;
  }

  @Override
  public boolean uses(Var v) {
    return v.equals(this);
  }

  @Override
  public String toString() {
    return name.toString();
  }

  @Override
  public StringBuilder stringify(StringBuilder builder) {
    return builder.append(name);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof BaseVar)) return false;
    final BaseVar that = (BaseVar) o;
    return this.name.equals(that.name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }
}
