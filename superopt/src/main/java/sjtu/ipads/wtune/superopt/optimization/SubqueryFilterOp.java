package sjtu.ipads.wtune.superopt.optimization;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.superopt.plan.OperatorType;
import sjtu.ipads.wtune.superopt.optimization.internal.SubqueryFilterOpImpl;

public interface SubqueryFilterOp extends FilterOp {
  @Override
  default OperatorType type() {
    return OperatorType.SubqueryFilter;
  }

  static SubqueryFilterOp make(ASTNode expr) {
    return SubqueryFilterOpImpl.build(expr);
  }
}
