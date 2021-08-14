package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.internal.InputNodeImpl;
import sjtu.ipads.wtune.sqlparser.schema.Table;

public interface InputNode extends PlanNode {
  Table table();

  ASTNode node();

  void setAlias(String alias);

  ASTNode tableSource();

  @Override
  default OperatorType kind() {
    return OperatorType.INPUT;
  }

  @Override
  default void resolveUsed() {}

  static InputNode make(ASTNode node, Table table, String alias) {
    return InputNodeImpl.build(node, table, alias);
  }
}
