package sjtu.ipads.wtune.prover.uexpr;

final class NotExprImpl extends UnaryExpr implements NotExpr {
  @Override
  protected UExprBase copy0() {
    return new NotExprImpl();
  }

  @Override
  public StringBuilder stringify(StringBuilder builder) {
    builder.append("not(");
    children[0].stringify(builder);
    return builder.append(')');
  }
}
