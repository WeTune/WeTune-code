package sjtu.ipads.wtune.superopt.optimizer;

import sjtu.ipads.wtune.common.utils.TreeScaffold;
import sjtu.ipads.wtune.sqlparser.plan.FilterNode;
import sjtu.ipads.wtune.sqlparser.plan.InSubFilterNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan.SimpleFilterNode;

import static sjtu.ipads.wtune.common.utils.TreeNode.treeRootOf;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.EXISTS_FILTER;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.IN_SUB_FILTER;

class DependentFilterConverter {
  /*
   * Currently, EXISTS filter is not supported during optimization.
   * This is workaround that turns EXISTS to SIMPLE filter.
   * */
  static PlanNode convert(PlanNode node) {
    if (!node.kind().isSubquery()) return node;
    final boolean isDependent =
        (node.kind() == EXISTS_FILTER)
            || (node.kind() == IN_SUB_FILTER && !((InSubFilterNode) node).rhsRefs().isEmpty());
    if (!isDependent) return node;

    final FilterNode subquery = (FilterNode) node;
    final FilterNode simpleFilter = SimpleFilterNode.mk(subquery.predicate(), subquery.refs());
    simpleFilter.setContext(subquery.context());

    final var scaffold = new TreeScaffold<>(treeRootOf(subquery));
    final var template = scaffold.rootTemplate();
    final var subTemplate = template.bindJointPoint(subquery, simpleFilter);
    subTemplate.bindJointPoint(simpleFilter, 0, subquery.predecessors()[0]);

    scaffold.instantiate();
    return subTemplate.getInstantiated();
  }
}
