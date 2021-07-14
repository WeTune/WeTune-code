package sjtu.ipads.wtune.prover.uexpr;

import com.google.common.base.Objects;

final class ConstVar implements Var {
  private final Name expr;

  ConstVar(Name expr) {
    this.expr = expr;
  }

  @Override
  public Var[] base() {
    return null;
  }

  @Override
  public Name name() {
    return expr;
  }

  @Override
  public Var subst(Var target, Var replacement) {
    return this;
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
    return expr.toString();
  }

  @Override
  public StringBuilder stringify(StringBuilder builder) {
    return builder.append(expr);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ConstVar)) return false;
    final ConstVar that = (ConstVar) o;
    return Objects.equal(expr, that.expr);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(expr);
  }
}
