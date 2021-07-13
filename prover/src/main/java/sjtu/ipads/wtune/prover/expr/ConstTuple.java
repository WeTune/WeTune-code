package sjtu.ipads.wtune.prover.expr;

import com.google.common.base.Objects;

final class ConstTuple implements Tuple {
  private final Name expr;

  ConstTuple(Name expr) {
    this.expr = expr;
  }

  @Override
  public Tuple[] base() {
    return null;
  }

  @Override
  public Name name() {
    return expr;
  }

  @Override
  public Tuple subst(Tuple target, Tuple replacement) {
    return this;
  }

  @Override
  public Tuple root() {
    return this;
  }

  @Override
  public boolean uses(Tuple v) {
    return v.equals(this);
  }

  @Override
  public String toString() {
    return expr.toString();
  }

  @Override
  public StringBuilder stringify(StringBuilder builder) {
    return builder.append(expr);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ConstTuple)) return false;
    final ConstTuple that = (ConstTuple) o;
    return Objects.equal(expr, that.expr);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(expr);
  }
}
