package sjtu.ipads.wtune.prover.expr;

final class NotExprImpl extends UnaryExpr implements NotExpr {
  @Override
  protected UExprBase copy0() {
    return new NotExprImpl();
  }

  @Override
  public String toString() {
    return "not(%s)".formatted(children[0]);
  }
}
