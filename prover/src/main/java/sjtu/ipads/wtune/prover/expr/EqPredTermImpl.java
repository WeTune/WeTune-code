package sjtu.ipads.wtune.prover.expr;

import java.util.Set;

import static java.util.Objects.requireNonNull;

class EqPredTermImpl extends UExprBase implements EqPredTerm {
  private Tuple left, right;

  public EqPredTermImpl(Tuple left, Tuple right) {
    this.left = requireNonNull(left);
    this.right = requireNonNull(right);
  }

  @Override
  public Tuple left() {
    return left;
  }

  @Override
  public Tuple right() {
    return right;
  }

  @Override
  public Set<Tuple> rootTuples() {
    return Set.of(left.root(), right.root());
  }

  @Override
  public void subst(Tuple v1, Tuple v2) {
    requireNonNull(v1);
    requireNonNull(v2);

    left = left.subst(v1, v2);
    right = right.subst(v1, v2);
  }

  @Override
  protected UExprBase copy0() {
    return new EqPredTermImpl(left, right);
  }

  @Override
  public String toString() {
    return "[%s = %s]".formatted(left, right);
  }
}
