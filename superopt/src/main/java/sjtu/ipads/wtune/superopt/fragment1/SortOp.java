package sjtu.ipads.wtune.superopt.fragment1;

class SortOp extends BaseOp implements Sort {
  SortOp() {}

  @Override
  public boolean accept0(OpVisitor visitor) {
    return visitor.enterSort(this);
  }

  @Override
  public void leave0(OpVisitor visitor) {
    visitor.leaveSort(this);
  }
}