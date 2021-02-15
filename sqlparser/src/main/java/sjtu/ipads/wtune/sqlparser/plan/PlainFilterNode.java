package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.internal.PlainFilterNodeImpl;

import java.util.List;

public interface PlainFilterNode extends FilterNode {
  @Override
  default OperatorType type() {
    return OperatorType.PlainFilter;
  }

  static PlainFilterNode make(ASTNode expr) {
    return PlainFilterNodeImpl.build(expr);
  }

  static PlainFilterNode make(ASTNode expr, List<OutputAttribute> usedAttrs) {
    return PlainFilterNodeImpl.build(expr, usedAttrs);
  }
}
