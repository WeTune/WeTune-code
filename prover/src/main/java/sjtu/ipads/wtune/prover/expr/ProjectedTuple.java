package sjtu.ipads.wtune.prover.expr;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Objects;

final class ProjectedTuple implements Tuple {
  private final Tuple[] base;
  private final Name projection;

  ProjectedTuple(Tuple base, Name projection) {
    this.base = new Tuple[] {requireNonNull(base)};
    this.projection = requireNonNull(projection);
  }

  @Override
  public Tuple[] base() {
    return base;
  }

  @Override
  public Name name() {
    return projection;
  }

  @Override
  public Tuple subst(Tuple t, Tuple rep) {
    requireNonNull(t);
    requireNonNull(rep);

    if (this.equals(t)) return rep;

    final Tuple base = this.base[0];
    final Tuple subst = base.subst(t, rep);
    if (subst == base) return this; // nothing happened
    else return new ProjectedTuple(subst, projection);
  }

  @Override
  public boolean uses(Tuple v) {
    return this.equals(v) || base[0].uses(v);
  }

  @Override
  public Tuple root() {
    return base[0].root();
  }

  @Override
  public String toString() {
    return base[0].toString() + '.' + projection;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ProjectedTuple)) return false;
    final ProjectedTuple that = (ProjectedTuple) o;
    return Objects.equal(base[0], that.base[0]) && Objects.equal(projection, that.projection);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(base[0], projection);
  }
}
