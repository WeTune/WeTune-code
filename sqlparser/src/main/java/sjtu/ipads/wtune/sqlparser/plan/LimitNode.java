package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.internal.LimitNodeImpl;

public interface LimitNode extends PlanNode {
  ASTNode limit();

  ASTNode offset();

  @Override
  default OperatorType kind() {
    return OperatorType.LIMIT;
  }

  static LimitNode make(ASTNode limit, ASTNode offset) {
    return LimitNodeImpl.build(limit, offset);
  }
}
