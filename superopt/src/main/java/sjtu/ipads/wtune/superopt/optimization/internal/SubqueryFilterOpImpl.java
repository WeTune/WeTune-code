package sjtu.ipads.wtune.superopt.optimization.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.superopt.optimization.SubqueryFilterOp;

public class SubqueryFilterOpImpl extends FilterOpBase implements SubqueryFilterOp {
  private SubqueryFilterOpImpl(ASTNode expr) {
    super(expr);
  }

  public static SubqueryFilterOp build(ASTNode expr) {
    return new SubqueryFilterOpImpl(expr);
  }
}
