package sjtu.ipads.wtune.superopt.optimization.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.superopt.optimization.LeftJoinOp;

public class LeftJoinOpImpl extends JoinOpBase implements LeftJoinOp {
  protected LeftJoinOpImpl(ASTNode onCondition) {
    super(onCondition);
  }

  public static LeftJoinOp build(ASTNode onCondition) {
    return new LeftJoinOpImpl(onCondition);
  }
}
