package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.internal.InnerJoinNodeImpl;

public interface InnerJoinNode extends JoinNode {
  @Override
  default OperatorType type() {
    return OperatorType.InnerJoin;
  }

  static InnerJoinNode build(ASTNode onCondition) {
    return InnerJoinNodeImpl.build(onCondition);
  }
}
