package sjtu.ipads.wtune.superopt.optimization;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.superopt.plan.OperatorType;
import sjtu.ipads.wtune.superopt.optimization.internal.FilterGroupOpImpl;

import java.util.List;

public interface FilterGroupOp extends FilterOp {
  List<FilterOp> filters();

  @Override
  default OperatorType type() {
    return OperatorType.PlainFilter;
  }

  static FilterGroupOp make(ASTNode expr, List<FilterOp> filters) {
    return FilterGroupOpImpl.build(expr, filters);
  }
}
