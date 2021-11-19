package sjtu.ipads.wtune.sqlparser.plan1;

public interface ExistsNode extends PlanNode {
  @Override
  default PlanKind kind() {
    return PlanKind.Exists;
  }

  static ExistsNode mk() {
    return new ExistsNodeImpl();
  }
}
