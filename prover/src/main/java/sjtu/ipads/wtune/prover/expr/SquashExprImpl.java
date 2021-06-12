package sjtu.ipads.wtune.prover.expr;

class SquashExprImpl extends UnaryExpr implements SquashExpr {
  @Override
  protected UExprBase copy0() {
    return new SquashExprImpl();
  }

  @Override
  public String toString() {
    return "|%s|".formatted(children[0]);
  }
}
