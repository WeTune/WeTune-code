package sjtu.ipads.wtune.prover.expr;

final class AddExprImpl extends BinaryExpr implements AddExpr {
  @Override
  protected UExprBase copy0() {
    return new AddExprImpl();
  }

  @Override
  public String toString() {
    if (children[1].kind() == Kind.ADD) return "%s + (%s)".formatted(children[0], children[1]);
    else return "%s + %s".formatted(children[0], children[1]);
  }
}
