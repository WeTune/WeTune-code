package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.internal.PlainFilterNodeImpl;

public interface PlainFilterNode extends FilterNode {
  @Override
  default OperatorType type() {
    return OperatorType.PlainFilter;
  }

  static PlainFilterNode make(ASTNode expr) {
    return PlainFilterNodeImpl.build(expr);
  }
}
