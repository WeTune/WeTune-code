package sjtu.ipads.wtune.superopt.optimization;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.superopt.plan.OperatorType;
import sjtu.ipads.wtune.superopt.optimization.internal.PlainFilterOpImpl;

public interface PlainFilterOp extends FilterOp {
  @Override
  default OperatorType type() {
    return OperatorType.PlainFilter;
  }

  static PlainFilterOp make(ASTNode expr) {
    return PlainFilterOpImpl.build(expr);
  }
}
