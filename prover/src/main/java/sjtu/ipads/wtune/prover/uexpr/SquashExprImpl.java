package sjtu.ipads.wtune.prover.uexpr;

final class SquashExprImpl extends UnaryExpr implements SquashExpr {
  @Override
  protected UExprBase copy0() {
    return new SquashExprImpl();
  }

  @Override
  public StringBuilder stringify(StringBuilder builder) {
    builder.append('|');
    children[0].stringify(builder);
    return builder.append('|');
  }
}
