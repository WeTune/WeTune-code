package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.internal.SubqueryFilterNodeImpl;

import java.util.List;

public interface SubqueryFilterNode extends FilterNode {
  @Override
  default OperatorType type() {
    return OperatorType.SubqueryFilter;
  }

  static SubqueryFilterNode make(ASTNode expr) {
    return SubqueryFilterNodeImpl.build(expr);
  }

  static SubqueryFilterNode make(List<PlanAttribute> usedAttrs) {
    return SubqueryFilterNodeImpl.build(usedAttrs);
  }
}
