package sjtu.ipads.wtune.sqlparser.plan1;

class InSubNodeImpl implements InSubNode {
  private final Expression expr;
  private boolean isPlain;

  InSubNodeImpl(Expression expr) {
    this.expr = expr;
    this.isPlain = true;
  }

  @Override
  public boolean isPlain() {
    return isPlain;
  }

  @Override
  public void setPlain(boolean plain) {
    isPlain = plain;
  }

  @Override
  public Expression expr() {
    return expr;
  }
}
