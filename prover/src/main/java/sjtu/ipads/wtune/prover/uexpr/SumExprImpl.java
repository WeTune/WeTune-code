package sjtu.ipads.wtune.prover.uexpr;

import static java.util.Objects.requireNonNull;

import java.util.List;
import sjtu.ipads.wtune.common.utils.Commons;

final class SumExprImpl extends UnaryExpr implements SumExpr {
  private final List<Var> boundVars;

  SumExprImpl(List<Var> boundVars) {
    this.boundVars = boundVars;
  }

  @Override
  public void subst(Var v1, Var v2) {
    requireNonNull(v1);
    if (boundVars.stream().noneMatch(v1::equals)) super.subst(v1, v2);
  }

  @Override
  public boolean uses(Var v) {
    return boundVars.stream().noneMatch(v::equals) && super.uses(v);
  }

  @Override
  protected UExprBase copy0() {
    return new SumExprImpl(boundVars);
  }

  @Override
  public StringBuilder stringify(StringBuilder builder) {
    builder.append('\u03a3');
    Commons.joining("{", ",", "}", false, boundVars, builder);
    builder.append('(');
    children[0].stringify(builder);
    builder.append(')');
    return builder;
  }

  @Override
  public List<Var> boundedVars() {
    return boundVars;
  }
}
