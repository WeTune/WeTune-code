package sjtu.ipads.wtune.sqlparser.plan1;

import sjtu.ipads.wtune.sqlparser.ast.constants.SetOperation;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;

public interface SetOpNode extends PlanNode {
  boolean distinct();

  SetOperation operation();

  @Override
  default OperatorType kind() {
    return OperatorType.UNION;
  }
}
