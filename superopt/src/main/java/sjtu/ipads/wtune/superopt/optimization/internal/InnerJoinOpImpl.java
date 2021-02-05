package sjtu.ipads.wtune.superopt.optimization.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.superopt.optimization.InnerJoinOp;

public class InnerJoinOpImpl extends JoinOpBase implements InnerJoinOp {
  protected InnerJoinOpImpl(ASTNode onCondition) {
    super(onCondition);
  }

  public static InnerJoinOp build(ASTNode onCondition) {
    return new InnerJoinOpImpl(onCondition);
  }
}
