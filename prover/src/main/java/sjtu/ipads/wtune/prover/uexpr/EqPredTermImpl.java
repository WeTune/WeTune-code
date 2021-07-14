package sjtu.ipads.wtune.prover.uexpr;

import static java.util.Objects.requireNonNull;

final class EqPredTermImpl extends UExprBase implements EqPredTerm {
  private Var left, right;

  public EqPredTermImpl(Var left, Var right) {
    this.left = requireNonNull(left);
    this.right = requireNonNull(right);
  }

  @Override
  public Var left() {
    return left;
  }

  @Override
  public Var right() {
    return right;
  }

  @Override
  public void subst(Var v1, Var v2) {
    requireNonNull(v1);
    requireNonNull(v2);

    left = left.subst(v1, v2);
    right = right.subst(v1, v2);
  }

  @Override
  public boolean uses(Var v) {
    return left.uses(v) || right.uses(v);
  }

  @Override
  protected UExprBase copy0() {
    return new EqPredTermImpl(left, right);
  }

  @Override
  public StringBuilder stringify(StringBuilder builder) {
    builder.append('[');
    left.stringify(builder).append(' ').append('=').append(' ');
    right.stringify(builder);
    return builder.append(']');
  }
}
