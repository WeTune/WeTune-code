package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.internal.InputNodeImpl;
import sjtu.ipads.wtune.sqlparser.relational.Relation;
import sjtu.ipads.wtune.sqlparser.schema.Table;

public interface InputNode extends PlanNode {
  Table table();

  ASTNode toTableSource();

  @Override
  default OperatorType type() {
    return OperatorType.Input;
  }

  static InputNode make(Relation table) {
    return InputNodeImpl.build(table);
  }
}
