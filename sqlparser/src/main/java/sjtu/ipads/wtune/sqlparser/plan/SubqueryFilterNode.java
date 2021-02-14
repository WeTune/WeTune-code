package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.internal.SubqueryFilterNodeImpl;

public interface SubqueryFilterNode extends FilterNode {
  @Override
  default OperatorType type() {
    return OperatorType.SubqueryFilter;
  }

  static SubqueryFilterNode make(ASTNode expr) {
    return SubqueryFilterNodeImpl.build(expr);
  }
}
