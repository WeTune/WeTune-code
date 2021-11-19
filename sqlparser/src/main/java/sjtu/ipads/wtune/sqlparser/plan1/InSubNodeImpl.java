package sjtu.ipads.wtune.sqlparser.plan1;

class InSubNodeImpl implements InSubNode {
  private final Expression expr;

  InSubNodeImpl(Expression expr) {
    this.expr = expr;
  }

  @Override
  public Expression expr() {
    return expr;
  }
}
