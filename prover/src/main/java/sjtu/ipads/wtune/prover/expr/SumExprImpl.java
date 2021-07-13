package sjtu.ipads.wtune.prover.expr;

import static java.util.Objects.requireNonNull;

import java.util.List;
import sjtu.ipads.wtune.common.utils.Commons;

final class SumExprImpl extends UnaryExpr implements SumExpr {
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
  public boolean uses(Tuple v) {
    return boundTuples.stream().noneMatch(v::equals) && super.uses(v);
  }

  @Override
  protected UExprBase copy0() {
    return new SumExprImpl(boundTuples);
  }

  @Override
  public StringBuilder stringify(StringBuilder builder) {
    builder.append('\u03a3');
    Commons.joining("{", ",", "}", false, boundTuples, builder);
    builder.append('(');
    children[0].stringify(builder);
    builder.append(')');
    return builder;
  }

  @Override
  public List<Tuple> boundedVars() {
    return boundTuples;
  }
}
