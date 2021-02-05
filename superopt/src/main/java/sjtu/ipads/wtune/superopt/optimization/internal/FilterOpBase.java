package sjtu.ipads.wtune.superopt.optimization.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.superopt.optimization.FilterOp;

public abstract class FilterOpBase extends OperatorBase implements FilterOp {
  private final ASTNode expr;

  protected FilterOpBase(ASTNode expr) {
    this.expr = expr;
  }

  @Override
  public ASTNode expr() {
    return expr;
  }
}
