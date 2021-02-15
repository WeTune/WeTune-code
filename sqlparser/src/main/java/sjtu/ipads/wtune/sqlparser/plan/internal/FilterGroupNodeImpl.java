package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.FilterGroupNode;
import sjtu.ipads.wtune.sqlparser.plan.FilterNode;
import sjtu.ipads.wtune.sqlparser.plan.OutputAttribute;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;

import java.util.List;

public class FilterGroupNodeImpl extends FilterNodeBase implements FilterGroupNode {
  private final List<FilterNode> filters;

  private FilterGroupNodeImpl(
      ASTNode expr, List<FilterNode> filters, List<OutputAttribute> attributes) {
    super(expr, attributes);
    this.filters = filters;
    for (int i = 0, bound = filters.size() - 1; i < bound; i++)
      filters.get(i).setPredecessor(0, filters.get(i + 1));
  }

  public static FilterGroupNode build(ASTNode expr, List<FilterNode> filters) {
    return new FilterGroupNodeImpl(expr, filters, null);
  }

  @Override
  public List<FilterNode> filters() {
    return filters;
  }

  @Override
  public void setPredecessor(int idx, PlanNode op) {
    super.setPredecessor(idx, op);
    filters.get(filters.size() - 1).setPredecessor(0, op);
  }

  @Override
  public void setSuccessor(PlanNode successor) {
    super.setSuccessor(successor);
    filters.get(0).setSuccessor(successor);
  }

  @Override
  public void resolveUsedAttributes() {
    super.resolveUsedAttributes();
    filters.forEach(PlanNode::resolveUsedAttributes);
  }

  @Override
  protected PlanNode copy0() {
    return new FilterGroupNodeImpl(expr, filters, usedAttributes);
  }
}
