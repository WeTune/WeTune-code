package sjtu.ipads.wtune.superopt.optimization;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.superopt.plan.OperatorType;
import sjtu.ipads.wtune.superopt.optimization.internal.InnerJoinOpImpl;

public interface InnerJoinOp extends JoinOp {
  @Override
  default OperatorType type() {
    return OperatorType.InnerJoin;
  }

  public static InnerJoinOp build(ASTNode onCondition) {
    return InnerJoinOpImpl.build(onCondition);
  }
}
