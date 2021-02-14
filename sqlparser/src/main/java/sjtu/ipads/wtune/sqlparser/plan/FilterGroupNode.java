package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.internal.FilterGroupNodeImpl;

import java.util.List;

public interface FilterGroupNode extends FilterNode {
  List<FilterNode> filters();

  @Override
  default OperatorType type() {
    return OperatorType.PlainFilter;
  }

  static FilterGroupNode make(ASTNode expr, List<FilterNode> filters) {
    return FilterGroupNodeImpl.build(expr, filters);
  }
}
