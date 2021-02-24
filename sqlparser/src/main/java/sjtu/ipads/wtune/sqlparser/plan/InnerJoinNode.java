package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.internal.InnerJoinNodeImpl;

import java.util.List;

public interface InnerJoinNode extends JoinNode {
  @Override
  default OperatorType type() {
    return OperatorType.InnerJoin;
  }

  static InnerJoinNode make(ASTNode onCondition) {
    return InnerJoinNodeImpl.build(onCondition);
  }

  static InnerJoinNode make(List<PlanAttribute> left, List<PlanAttribute> right) {
    return InnerJoinNodeImpl.build(left, right);
  }
}
