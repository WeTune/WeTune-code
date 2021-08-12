package sjtu.ipads.wtune.prover.uexpr;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Objects;

final class ProjectedVar implements Var {
  private final Var[] base;
  private final Name projection;

  ProjectedVar(Var base, Name projection) {
    this.base = new Var[] {requireNonNull(base)};
    this.projection = requireNonNull(projection);
  }

  @Override
  public Var[] base() {
    return base;
  }

  @Override
  public Name name() {
    return projection;
  }

  @Override
  public Var subst(Var t, Var rep) {
    requireNonNull(t);
    requireNonNull(rep);

    if (this.equals(t)) return rep;

    final Var base = this.base[0];
    final Var subst = base.subst(t, rep);
    if (subst == base) return this; // nothing happened
    else return new ProjectedVar(subst, projection);
  }

  @Override
  public boolean uses(Var v) {
    return this.equals(v) || base[0].uses(v);
  }

  @Override
  public Var root() {
    return base[0].root();
  }

  @Override
  public String toString() {
    return base[0].toString() + '.' + projection;
  }

  @Override
  public StringBuilder stringify(StringBuilder builder) {
    return base[0].stringify(builder).append('.').append(projection);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ProjectedVar)) return false;
    final ProjectedVar that = (ProjectedVar) o;
    return Objects.equal(base[0], that.base[0]) && Objects.equal(projection, that.projection);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(base[0], projection);
  }
}
