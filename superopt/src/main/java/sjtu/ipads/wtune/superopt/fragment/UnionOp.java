package sjtu.ipads.wtune.superopt.fragment;

class UnionOp extends BaseOp implements Union {
  UnionOp() {}

  @Override
  public boolean accept0(OpVisitor visitor) {
    return visitor.enterUnion(this);
  }

  @Override
  public void leave0(OpVisitor visitor) {
    visitor.leaveUnion(this);
  }
}
