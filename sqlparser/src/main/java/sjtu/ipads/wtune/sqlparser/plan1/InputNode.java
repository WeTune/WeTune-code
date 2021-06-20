package sjtu.ipads.wtune.sqlparser.plan1;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.schema.Table;

public interface InputNode extends PlanNode {
  Table table();

  @Override
  default OperatorType type() {
    return OperatorType.Input;
  }
}
