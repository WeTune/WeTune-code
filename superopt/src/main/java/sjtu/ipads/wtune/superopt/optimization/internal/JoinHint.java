package sjtu.ipads.wtune.superopt.optimization.internal;

import sjtu.ipads.wtune.sqlparser.plan.InnerJoinNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanAttribute;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.schema.Column;
import sjtu.ipads.wtune.superopt.fragment.InnerJoin;
import sjtu.ipads.wtune.superopt.fragment.symbolic.AttributeInterpretation;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Interpretations;

import java.util.ArrayList;
import java.util.List;

import static sjtu.ipads.wtune.common.utils.Commons.tail;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.plan.PlanNode.*;

public class JoinHint {
  public static Iterable<PlanNode> rearrangeJoin(
      InnerJoinNode originRoot, InnerJoin op, Interpretations inter) {
    // premise: the join tree is always kept left-deep
    // This function tries to shift each join node in join tree as the root
    final List<InnerJoinNode> joins = collectInnerJoinNodes(originRoot);
    final List<PlanNode> joinTrees = new ArrayList<>(joins.size() + 1);

    final PlanNode successor = originRoot.successor();

    for (int i = 0, bound = joins.size(); i <= bound; i++) {
      final InnerJoinNode joinTree = rebuildJoinTree(joins, i);
      if (!isValidJoin(joinTree)) continue;

      resolveUsedAttributes(joinTree);
      if (!isEligibleMatch(joinTree, op, inter)) continue;

      if (successor != null) copyToRoot(successor).replacePredecessor(originRoot, joinTree);

      resolveUsedAttributes(rootOf(joinTree));
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
    final boolean shiftLeaves = rootIdx >= nodes.size();
    rootIdx = Math.min(rootIdx, nodes.size() - 1);

    // use nodes[rootIdx] as root, construct a left-deep join tree with remaining nodes
    final InnerJoinNode root = (InnerJoinNode) nodes.get(rootIdx).copy();
    root.setPredecessor(1, copyTree(root.predecessors()[1]));

    PlanNode successor = root;
    for (int i = 0, bound = nodes.size(); i < bound; i++) {
      if (i == rootIdx) continue;

      final PlanNode current = nodes.get(i).copy();
      current.setPredecessor(1, copyTree(current.predecessors()[1]));
      successor.setPredecessor(0, current);
      successor = current;
    }
    successor.setPredecessor(0, copyTree(tail(nodes).predecessors()[0]));

    if (shiftLeaves) {
      final PlanNode rightMostLeaf = root.predecessors()[1];
      final PlanNode leftMostLeaf = successor.predecessors()[0];
      root.setPredecessor(1, leftMostLeaf);
      successor.setPredecessor(0, rightMostLeaf);
    }

    return root;
  }

  private static boolean isValidJoin(PlanNode node) {
    if (!(node instanceof InnerJoinNode)) return true;
    final InnerJoinNode join = (InnerJoinNode) node;
    // check if all attributes are presented in input
    for (PlanAttribute attr : join.usedAttributes())
      if (join.resolveAttribute(attr) == null) return false;
    return isValidJoin(join.predecessors()[0]);
  }

  private static boolean isEligibleMatch(InnerJoinNode node, InnerJoin op, Interpretations inter) {
    final AttributeInterpretation leftInter = inter.getAttributes(op.leftFields());
    final AttributeInterpretation rightInter = inter.getAttributes(op.rightFields());

    // check whether compatible with existing assignment
    if (leftInter != null && !leftInter.isCompatible(node.leftAttributes())) return false;
    if (rightInter != null && !rightInter.isCompatible(node.rightAttributes())) return false;

    // check whether the Reference constraint can be enforced
    if (inter.constraints().requiresReference(op.leftFields(), op.rightFields())) {
      final List<Column> referred = listMap(it -> it.column(true), node.rightAttributes());
      for (PlanAttribute referee : node.leftAttributes()) {
        final Column column = referee.column(true);
        if (column == null || !column.references(referred)) return false;
      }
    }

    return true;
  }
}
