package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.internal.PlainFilterNodeImpl;

import java.util.List;
import java.util.Set;

public interface PlainFilterNode extends FilterNode {
  Set<AttributeDef> fixedValueAttributes();

  @Override
  default OperatorType type() {
    return OperatorType.PlainFilter;
  }

  static PlainFilterNode make(ASTNode expr) {
    return PlainFilterNodeImpl.build(expr);
  }

  static PlainFilterNode make(ASTNode expr, List<AttributeDef> usedAttrs) {
    return PlainFilterNodeImpl.build(expr, usedAttrs);
  }
}
