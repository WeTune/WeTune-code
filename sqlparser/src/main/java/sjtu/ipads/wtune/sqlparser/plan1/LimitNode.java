package sjtu.ipads.wtune.sqlparser.plan1;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;

public interface LimitNode extends PlanNode {
  Expr limit();

  Expr offset();

  @Override
  default OperatorType type() {
    return OperatorType.Limit;
  }
}
