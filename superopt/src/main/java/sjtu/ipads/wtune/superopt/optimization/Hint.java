package sjtu.ipads.wtune.superopt.optimization;

import sjtu.ipads.wtune.sqlparser.plan.*;
import sjtu.ipads.wtune.sqlparser.schema.Column;
import sjtu.ipads.wtune.superopt.fragment.InnerJoin;
import sjtu.ipads.wtune.superopt.fragment.Operator;
import sjtu.ipads.wtune.superopt.fragment.symbolic.AttributeInterpretation;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Interpretations;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

public interface Hint {
  public static Iterable<PlanNode> apply(PlanNode node, Operator op, Interpretations inter) {
    if (op.type() == OperatorType.Input) return singletonList(node);
    if (node.type() != op.type()) return emptyList();

    if (node instanceof FilterGroupNode) return rearrangeFilter((FilterGroupNode) node, op, inter);
    else if (node instanceof InnerJoinNode)
      return rearrangeJoin((InnerJoinNode) node, ((InnerJoin) op), inter);
    else return singletonList(node);
  }

  private static Iterable<PlanNode> rearrangeFilter(
      FilterGroupNode filter, Operator op, Interpretations inter) {
    return singletonList(filter.filters().get(0));
  }

  private static Iterable<PlanNode> rearrangeJoin(
      InnerJoinNode join, InnerJoin op, Interpretations inter) {
    // premise: the join tree is always kept left-deep
    // This function tries to shift each join node in join tree as the root
    final List<InnerJoinNode> joins = collectInnerJoinNodes(join);
    final List<PlanNode> joinTrees = new ArrayList<>(joins.size() + 1);

    for (int i = 0, bound = joins.size(); i <= bound; i++) {
      final InnerJoinNode joinTree = rebuildJoinTree(joins, i);
      if (!isValidJoin(joinTree)) continue;

      collectInnerJoinNodes(joinTree).forEach(PlanNode::resolveUsedAttributes);

      if (!isEligibleMatching(joinTree, op, inter)) continue;

      joinTrees.add(joinTree);
    }

    return joinTrees;
  }

  private static List<InnerJoinNode> collectInnerJoinNodes(PlanNode node) {
    final List<InnerJoinNode> joins = new ArrayList<>();

    while (node instanceof InnerJoinNode) {
      joins.add((InnerJoinNode) node);
      node = node.predecessors()[0];
    }

    return joins;
  }

  private static InnerJoinNode rebuildJoinTree(List<InnerJoinNode> nodes, int rootIdx) {
    final boolean shiftTrailingNode = rootIdx >= nodes.size();
    rootIdx = Math.min(rootIdx, nodes.size() - 1);

    // use nodes[rootIdx] as root, construct a left-deep join tree with remaining nodes
    final InnerJoinNode root = (InnerJoinNode) nodes.get(rootIdx).copy();

    PlanNode prev = root;
    for (int i = 0, bound = nodes.size(); i < bound; i++) {
      if (i == rootIdx) continue;
      final PlanNode cur = nodes.get(i).copy();
      prev.setPredecessor(0, cur);
      prev = cur;
    }
    prev.setPredecessor(0, nodes.get(nodes.size() - 1).predecessors()[0]);

    if (shiftTrailingNode) {
      final PlanNode toShift0 = root.predecessors()[1];
      final PlanNode toShift1 = prev.predecessors()[0];
      root.setPredecessor(1, toShift1);
      prev.setPredecessor(0, toShift0);
    }

    return root;
  }

  private static boolean isValidJoin(PlanNode node) {
    if (!(node instanceof InnerJoinNode)) return true;
    final InnerJoinNode join = (InnerJoinNode) node;
    // check if all attributes are presented in input
    for (OutputAttribute attr : join.usedAttributes())
      if (join.resolveAttribute(attr) == null) return false;
    return isValidJoin(join.predecessors()[0]);
  }

  private static boolean isEligibleMatching(
      InnerJoinNode node, InnerJoin op, Interpretations inter) {
    final AttributeInterpretation leftInter = inter.getAttributes(op.leftFields());
    final AttributeInterpretation rightInter = inter.getAttributes(op.rightFields());

    // check whether compatible with existing assignment
    if (leftInter != null && !leftInter.isCompatible(node.leftAttributes())) return false;
    if (rightInter != null && !rightInter.isCompatible(node.rightAttributes())) return false;

    // check whether the Reference constraint can be enforced
    if (inter.constraints().requiresReference(op.leftFields(), op.rightFields())) {
      final List<Column> referred = listMap(it -> it.column(true), node.rightAttributes());
      for (OutputAttribute referee : node.leftAttributes()) {
        final Column column = referee.column(true);
        if (column == null || !column.references(referred)) return false;
      }
    }

    return true;
  }
}
