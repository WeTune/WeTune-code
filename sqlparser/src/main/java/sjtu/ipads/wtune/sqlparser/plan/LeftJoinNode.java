package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.internal.LeftJoinNodeImpl;

public interface LeftJoinNode extends JoinNode {
  @Override
  default OperatorType type() {
    return OperatorType.LeftJoin;
  }

  static LeftJoinNode build(ASTNode onCondition) {
    return LeftJoinNodeImpl.build(onCondition);
  }
}
