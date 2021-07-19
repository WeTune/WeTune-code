package sjtu.ipads.wtune.superopt.fragment1;

class ProjOp extends BaseOp implements Proj {
  ProjOp() {}

  @Override
  public Symbol attrs() {
    return fragment().symbols().symbolAt(this, Symbol.Kind.ATTRS, 0);
  }

  @Override
  public boolean accept0(OpVisitor visitor) {
    return visitor.enterProj(this);
  }

  @Override
  public void leave0(OpVisitor visitor) {
    visitor.leaveProj(this);
  }
}
