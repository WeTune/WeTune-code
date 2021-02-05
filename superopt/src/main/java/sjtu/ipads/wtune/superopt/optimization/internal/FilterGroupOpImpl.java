package sjtu.ipads.wtune.superopt.optimization.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.superopt.optimization.FilterGroupOp;
import sjtu.ipads.wtune.superopt.optimization.FilterOp;

import java.util.List;

public class FilterGroupOpImpl extends FilterOpBase implements FilterGroupOp {
  private final List<FilterOp> filters;

  private FilterGroupOpImpl(ASTNode expr, List<FilterOp> filters) {
    super(expr);
    this.filters = filters;
  }

  public static FilterGroupOp build(ASTNode expr, List<FilterOp> filters) {
    return new FilterGroupOpImpl(expr, filters);
  }

  @Override
  public List<FilterOp> filters() {
    return filters;
  }
}
