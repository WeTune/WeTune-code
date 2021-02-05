package sjtu.ipads.wtune.superopt.optimization;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.superopt.plan.OperatorType;
import sjtu.ipads.wtune.superopt.optimization.internal.LeftJoinOpImpl;

public interface LeftJoinOp extends JoinOp {
  @Override
  default OperatorType type() {
    return OperatorType.LeftJoin;
  }

  static LeftJoinOp build(ASTNode onCondition) {
    return LeftJoinOpImpl.build(onCondition);
  }
}
