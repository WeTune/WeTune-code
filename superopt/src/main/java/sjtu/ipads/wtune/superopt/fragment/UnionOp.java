package sjtu.ipads.wtune.superopt.fragment;

class UnionOp extends BaseOp implements Union {
  boolean isDeduplicated = false;
  UnionOp() {}

  @Override
  public void setDeduplicated(boolean flag) {
    this.isDeduplicated = flag;
  }

  @Override
  public boolean isDeduplicated() {
    return isDeduplicated;
  }

  @Override
  public boolean accept0(OpVisitor visitor) {
    return visitor.enterUnion(this);
  }

  @Override
  public void leave0(OpVisitor visitor) {
    visitor.leaveUnion(this);
  }
}
