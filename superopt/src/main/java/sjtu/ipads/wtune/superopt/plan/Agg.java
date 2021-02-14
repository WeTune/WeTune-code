package sjtu.ipads.wtune.superopt.plan;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.superopt.plan.internal.AggImpl;

public interface Agg extends PlanNode {
  static Agg create() {
    return AggImpl.create();
  }

  @Override
  default OperatorType type() {
    return OperatorType.Agg;
  }
}
