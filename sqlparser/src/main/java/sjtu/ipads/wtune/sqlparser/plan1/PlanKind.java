package sjtu.ipads.wtune.sqlparser.plan1;

public enum PlanKind {
  Input(0),
  Proj(1),
  Filter(1),
  InSub(2),
  Exists(2),
  Join(2),
  Agg(1),
  SetOp(2),
  Limit(1),
  Sort(1);

  private final int numChildren;

  PlanKind(int numChildren) {
    this.numChildren = numChildren;
  }

  public boolean isFilter() {
    return this == Filter || this == InSub || this == Exists;
  }

  public int numChildren() {
    return numChildren;
  }
}
