package sjtu.ipads.wtune.sqlparser.plan;

public interface ProjNode extends PlanNode {
  boolean containsWildcard();

  boolean isDeduplicated();
  // For wildcard resolution.
  // If there are no wildcard, the values are immutable.
  // Note: the bag is expected containing only ExprValue.
  void setValues(ValueBag bag);

  void setDeduplicated(boolean explicitDistinct);

  @Override
  default OperatorType kind() {
    return OperatorType.PROJ;
  }

  static ProjNode mk(ValueBag outValues) {
    return ProjNodeImpl.mk(outValues);
  }

  static ProjNode mkWildcard(ValueBag inValues) {
    return ProjNodeImpl.mkWildcard(inValues);
  }
}
