package sjtu.ipads.wtune.sqlparser.plan1;

import static sjtu.ipads.wtune.common.utils.Commons.coalesce;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.schema.Table;

public interface InputNode extends PlanNode {
  Table table();

  @Override
  default OperatorType type() {
    return OperatorType.INPUT;
  }

  static InputNode mk(Table table, String alias) {
    return new InputNodeImpl(table, coalesce(alias, table.name()));
  }
}
