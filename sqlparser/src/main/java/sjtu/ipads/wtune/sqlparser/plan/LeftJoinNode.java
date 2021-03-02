package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.internal.LeftJoinNodeImpl;

import java.util.List;

public interface LeftJoinNode extends JoinNode {
  @Override
  default OperatorType type() {
    return OperatorType.LeftJoin;
  }

  static LeftJoinNode make(ASTNode onCondition) {
    return LeftJoinNodeImpl.build(onCondition);
  }

  static LeftJoinNode make(List<AttributeDef> left, List<AttributeDef> right) {
    return LeftJoinNodeImpl.build(left, right);
  }
}
