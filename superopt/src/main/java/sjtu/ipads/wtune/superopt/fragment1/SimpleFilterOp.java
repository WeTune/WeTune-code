package sjtu.ipads.wtune.superopt.fragment1;

public class SimpleFilterOp extends BaseOp implements SimpleFilter {
  @Override
  public Symbol attrs() {
    return fragment().symbols().symbolAt(this, Symbol.Kind.ATTRS, 0);
  }

  @Override
  public Symbol predicate() {
    return fragment().symbols().symbolAt(this, Symbol.Kind.PRED, 0);
  }

  @Override
  public boolean accept0(OpVisitor visitor) {
    return visitor.enterPlainFilter(this);
  }

  @Override
  public void leave0(OpVisitor visitor) {
    visitor.leaveSimpleFilter(this);
  }
}
