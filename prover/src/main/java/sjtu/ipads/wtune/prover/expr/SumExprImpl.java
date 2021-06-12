package sjtu.ipads.wtune.prover.expr;

class SumExprImpl extends UnaryExpr implements SumExpr {
  @Override
  protected UExprBase copy0() {
    return new SumExprImpl();
  }

  @Override
  public String toString() {
    return "sum(%s)".formatted(children[0]);
  }
}
