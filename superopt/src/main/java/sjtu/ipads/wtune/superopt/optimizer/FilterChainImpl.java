package sjtu.ipads.wtune.superopt.optimizer;

import sjtu.ipads.wtune.common.utils.TreeScaffold;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp;
import sjtu.ipads.wtune.sqlparser.plan.*;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

import static sjtu.ipads.wtune.common.utils.TreeNode.treeRootOf;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.*;
import static sjtu.ipads.wtune.sqlparser.util.ColumnRefCollector.gatherColumnRefs;

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
    final PlanNode successor = successorOfChain(chainHead);
    final PlanNode predecessor = predecessorOfChain(chainHead);
    return mk(successor, predecessor, filters);
  }

  private static PlanNode successorOfChain(FilterNode chainHead) {
    return chainHead.successor();
  }

  private static PlanNode predecessorOfChain(FilterNode chainHead) {
    PlanNode path = chainHead;
    while (path.kind().isFilter()) path = path.predecessors()[0];
    return path;
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
      if (!expandCombination) filters.add(path);
      else {
        if (path instanceof CombinedFilterNode)
          filters.addAll(((CombinedFilterNode) path).filters());
        else if (isCombinedExpr(path)) filters.addAll(splitCombinedPredicate(path));
        else filters.add(path);
      }

      if (path.predecessors()[0].kind().isFilter()) path = (FilterNode) path.predecessors()[0];
      else break;
    }

    return filters;
  }

  private static boolean isCombinedExpr(FilterNode filter) {
    return filter.predicate().template().get(BINARY_OP) == BinaryOp.AND;
  }

  private static List<FilterNode> splitCombinedPredicate(FilterNode filter) {
    final Expr predicate = filter.predicate();
    final List<ASTNode> nodes = new ArrayList<>(4);
    splitConjunction(predicate.template(), nodes);

    final PlanContext ctx = filter.context();
    final List<FilterNode> filters = new ArrayList<>(nodes.size());
    final List<Ref> refs = filter.refs();
    int index = 0;
    for (ASTNode node : nodes) {
      final int numRefs = gatherColumnRefs(node).size();
      final RefBag usedRefs = RefBag.mk(refs.subList(index, index + numRefs));
      final Expr expr = Expr.mk(node, usedRefs);
      final SimpleFilterNode newFilter = SimpleFilterNode.mk(expr, usedRefs);
      newFilter.setContext(ctx);
      filters.add(newFilter);
      index += numRefs;
    }
    return filters;
  }

  private static void splitConjunction(ASTNode node, List<ASTNode> nodes) {
    if (node.get(BINARY_OP) != BinaryOp.AND) nodes.add(node.deepCopy());
    else {
      splitConjunction(node.get(BINARY_LEFT), nodes);
      splitConjunction(node.get(BINARY_RIGHT), nodes);
    }
  }
}
