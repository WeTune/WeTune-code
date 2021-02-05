package sjtu.ipads.wtune.superopt.optimization.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.superopt.optimization.JoinOp;

public abstract class JoinOpBase extends OperatorBase implements JoinOp {
  private final ASTNode onCondition;

  protected JoinOpBase(ASTNode onCondition) {
    this.onCondition = onCondition;
  }

  @Override
  public ASTNode onCondition() {
    return onCondition;
  }
}
