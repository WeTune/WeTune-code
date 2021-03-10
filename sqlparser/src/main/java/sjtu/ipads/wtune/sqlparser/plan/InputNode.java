package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.internal.InputNodeImpl;
import sjtu.ipads.wtune.sqlparser.schema.Table;

public interface InputNode extends PlanNode {
  ASTNode toTableSource();

  Table table();

  int id();

  void setAlias(String alias);

  @Override
  default OperatorType type() {
    return OperatorType.Input;
  }

  @Override
  default void resolveUsed() {}

  static InputNode make(Table table, String alias) {
    return InputNodeImpl.build(table, alias);
  }
}
