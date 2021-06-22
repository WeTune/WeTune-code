package sjtu.ipads.wtune.prover.expr;

import java.util.List;

import static java.util.Objects.requireNonNull;

class SumExprImpl extends UnaryExpr implements SumExpr {
  private final List<Tuple> boundTuples;

  SumExprImpl(List<Tuple> boundTuples) {
    this.boundTuples = boundTuples;
  }

  @Override
  public void subst(Tuple v1, Tuple v2) {
    requireNonNull(v1);
    if (boundTuples.stream().noneMatch(v1::equals)) super.subst(v1, v2);
  }

  @Override
  protected UExprBase copy0() {
    return new SumExprImpl(boundTuples);
  }

  @Override
  public String toString() {
    return "sum%s(%s)".formatted(boundTuples, children[0]);
  }

  @Override
  public List<Tuple> boundedVars() {
    return boundTuples;
  }
}
