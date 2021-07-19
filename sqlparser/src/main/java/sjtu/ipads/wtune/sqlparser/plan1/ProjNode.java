package sjtu.ipads.wtune.sqlparser.plan1;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;

public interface ProjNode extends PlanNode {
  boolean containsWildcard();

  boolean isExplicitDistinct();
  // For wildcard resolution.
  // If there are no wildcard, the values are immutable.
  // Note: the bag is expected containing only ExprValue.
  void setValues(ValueBag bag);

  void setExplicitDistinct(boolean explicitDistinct);

  @Override
  default OperatorType type() {
    return OperatorType.PROJ;
  }

  static ProjNode mk(ValueBag values) {
    return ProjNodeImpl.mk(values);
  }
}
