package sjtu.ipads.wtune.superopt.fragment1;

class InSubFilterOp extends BaseOp implements InSubFilter {
  InSubFilterOp() {}

  @Override
  public Symbol attrs() {
    return fragment().symbols().symbolAt(this, Symbol.Kind.ATTRS, 0);
  }

  @Override
  public boolean accept0(OpVisitor visitor) {
    return visitor.enterSubqueryFilter(this);
  }

  @Override
  public void leave0(OpVisitor visitor) {
    visitor.leaveInSubFilter(this);
  }
}
