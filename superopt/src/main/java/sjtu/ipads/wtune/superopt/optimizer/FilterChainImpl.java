package sjtu.ipads.wtune.superopt.optimizer;

import sjtu.ipads.wtune.common.utils.Commons;
import sjtu.ipads.wtune.common.utils.TreeScaffold;
import sjtu.ipads.wtune.sqlparser.plan.CombinedFilterNode;
import sjtu.ipads.wtune.sqlparser.plan.FilterNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

import static sjtu.ipads.wtune.common.utils.TreeNode.treeRootOf;

class FilterChainImpl extends AbstractList<FilterNode> implements FilterChain {
  private final PlanNode successor, predecessor;
  private final List<FilterNode> filters;

  private FilterChainImpl(PlanNode successor, PlanNode predecessor, List<FilterNode> filters) {
    this.successor = successor;
    this.predecessor = predecessor;
    this.filters = filters;
  }

  static FilterChain mk(PlanNode successor, PlanNode predecessor, List<FilterNode> filters) {
    return new FilterChainImpl(successor, predecessor, filters);
  }

  static FilterChain mk(FilterNode chainHead, boolean expandCombination) {
    final List<FilterNode> filters = linearizeChain(chainHead, expandCombination);
    final PlanNode successor = chainHead.successor();
    final PlanNode predecessor = Commons.tail(filters).predecessors()[0];
    return mk(successor, predecessor, filters);
  }

  @Override
  public FilterNode get(int index) {
    return filters.get(index);
  }

  @Override
  public int size() {
    return filters.size();
  }

  @Override
  public PlanNode successor() {
    return successor;
  }

  @Override
  public PlanNode predecessor() {
    return predecessor;
  }

  @Override
  public FilterNode buildChain() {
    final var scaffold = new TreeScaffold<>(treeRootOf(successor));
    var rootTemplate = scaffold.rootTemplate();
    var chainTemplate = rootTemplate.bindJointPoint(successor, 0, filters.get(0));
    var template = chainTemplate;

    for (int i = 1, bound = filters.size(); i < bound; i++)
      template = template.bindJointPoint(filters.get(i - 1), 0, filters.get(i));
    template.bindJointPoint(filters.get(filters.size() - 1), 0, predecessor);

    scaffold.instantiate();
    return (FilterNode) chainTemplate.getInstantiated();
  }

  private static List<FilterNode> linearizeChain(FilterNode chainHead, boolean expandCombination) {
    final List<FilterNode> filters = new ArrayList<>();

    FilterNode path = chainHead;
    while (true) {
      if (path instanceof CombinedFilterNode && expandCombination)
        filters.addAll(((CombinedFilterNode) path).filters());
      else filters.add(path);

      if (path.predecessors()[0].kind().isFilter()) path = (FilterNode) path.predecessors()[0];
      else break;
    }

    return filters;
  }
}
