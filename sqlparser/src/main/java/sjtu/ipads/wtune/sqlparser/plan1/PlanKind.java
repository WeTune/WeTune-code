package sjtu.ipads.wtune.sqlparser.plan1;

public enum PlanKind {
  Input,
  Proj,
  Filter,
  InSub,
  Exists,
  Join,
  Agg,
  SetOp,
  Limit,
  Sort;

  public boolean isFilter() {
    return this == Filter || this == InSub || this == Exists;
  }
}
