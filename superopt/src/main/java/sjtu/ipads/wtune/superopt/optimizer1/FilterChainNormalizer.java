package sjtu.ipads.wtune.superopt.optimizer1;

import com.google.common.collect.Lists;
import sjtu.ipads.wtune.common.utils.TreeScaffold;
import sjtu.ipads.wtune.sqlparser.plan1.CombinedFilterNode;
import sjtu.ipads.wtune.sqlparser.plan1.FilterNode;
import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;

import java.util.ArrayList;
import java.util.List;

import static sjtu.ipads.wtune.common.utils.TreeNode.treeRootOf;

class FilterChainNormalizer {
  static FilterNode normalize(FilterNode node) {
    return (FilterNode) normalize0(node, true);
  }

  private static PlanNode normalize0(PlanNode node, boolean isRoot) {
    if (node.kind().isFilter()
        && (node.successor() == null || !node.successor().kind().isFilter())) {
      final FilterNode chainHead = (FilterNode) node;
      if (containsCombinedFilterNode(chainHead)) {
        final List<FilterNode> chain = expandFilterChain(chainHead);
        final PlanNode successor = node.successor();
        final PlanNode predecessor = predecessorOfChain(chainHead);

        final var scaffold = new TreeScaffold<>(treeRootOf(node));
        var rootTemplate = scaffold.rootTemplate();
        var chainTemplate = rootTemplate.bindJointPoint(successor, 0, chain.get(0));
        var template = chainTemplate;
        for (int i = 1, bound = chain.size(); i < bound; i++)
          template = template.bindJointPoint(chain.get(i - 1), 0, chain.get(i));
        template.bindJointPoint(chain.get(chain.size() - 1), 0, predecessor);

        scaffold.instantiate();
        node = chainTemplate.getInstantiated();
      }
    }

    for (int i = 0, bound = node.kind().numPredecessors(); i < bound; i++)
      node = normalize0(node.predecessors()[i], false);

    return isRoot ? node : node.successor();
  }

  private static boolean containsCombinedFilterNode(FilterNode chainHead) {
    PlanNode path = chainHead;
    while (path.kind().isFilter()) {
      if (path instanceof CombinedFilterNode) return true;
      path = path.predecessors()[0];
    }
    return false;
  }

  private static List<FilterNode> expandFilterChain(FilterNode chainHead) {
    final List<FilterNode> filters = new ArrayList<>();
    FilterNode path = chainHead;
    while (true) {
      if (path instanceof CombinedFilterNode) filters.addAll(((CombinedFilterNode) path).filters());
      else filters.add(path);
      if (path.predecessors()[0].kind().isFilter()) path = (FilterNode) path.predecessors()[0];
      else break;
    }
    return Lists.reverse(filters);
  }

  private static PlanNode predecessorOfChain(FilterNode chainHead) {
    PlanNode path = chainHead;
    while (path.kind().isFilter()) path = path.predecessors()[0];
    return path;
  }
}
