package sjtu.ipads.wtune.prover.expr;

final class MulExprImpl extends BinaryExpr implements MulExpr {
  @Override
  protected UExprBase copy0() {
    return new MulExprImpl();
  }

  @Override
  public String toString() {
    final UExpr c0 = children[0];
    final UExpr c1 = children[1];

    final String piece0 = c0.kind() == Kind.ADD ? "(%s)" : "%s";
    final String piece1 = c1.kind() == Kind.ADD || c1.kind() == Kind.MUL ? "(%s)" : "%s";
    return (piece0 + " * " + piece1).formatted(c0, c1);
  }
}
