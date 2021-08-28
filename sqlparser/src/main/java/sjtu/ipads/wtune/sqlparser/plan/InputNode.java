package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.schema.Table;

import static sjtu.ipads.wtune.common.utils.Commons.coalesce;

public interface InputNode extends PlanNode {
  Table table();

  @Override
  default OperatorType kind() {
    return OperatorType.INPUT;
  }

  static InputNode mk(Table table, String alias) {
    return new InputNodeImpl(table, coalesce(alias, table.name()));
  }
}
