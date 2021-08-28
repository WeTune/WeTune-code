package sjtu.ipads.wtune.superopt.fragment;

class AggOp extends BaseOp implements Agg {
  AggOp() {}

  @Override
  public boolean accept0(OpVisitor visitor) {
    return visitor.enterAgg(this);
  }

  @Override
  public void leave0(OpVisitor visitor) {
    visitor.leaveAgg(this);
  }
}
