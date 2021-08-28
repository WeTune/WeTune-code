package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.ast.constants.SetOperation;

public interface SetOpNode extends PlanNode {
  boolean distinct();

  SetOperation operation();

  @Override
  default OperatorType kind() {
    return OperatorType.UNION;
  }
}
