package sjtu.ipads.wtune.superopt.optimization.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.superopt.optimization.PlainFilterOp;

public class PlainFilterOpImpl extends FilterOpBase implements PlainFilterOp {
  private PlainFilterOpImpl(ASTNode expr) {
    super(expr);
  }

  public static PlainFilterOp build(ASTNode expr) {
    return new PlainFilterOpImpl(expr);
  }
}
