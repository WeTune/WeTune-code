package sjtu.ipads.wtune.superopt.optimization.internal;

import com.google.common.collect.Lists;
import sjtu.ipads.wtune.sqlparser.plan.*;
import sjtu.ipads.wtune.sqlparser.schema.Column;
import sjtu.ipads.wtune.superopt.fragment.InnerJoin;
import sjtu.ipads.wtune.superopt.fragment.Join;
import sjtu.ipads.wtune.superopt.fragment.LeftJoin;
import sjtu.ipads.wtune.superopt.fragment.symbolic.AttributeInterpretation;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Interpretations;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.common.utils.Commons.isEmpty;
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
      final JoinNode joinTree = rebuildJoinTree(joins, i);
      if (!isValidJoin(joinTree)) continue;

      resolveUsedTree(joinTree);
      if (!isEligibleMatch(joinTree, op, inter)) continue;

      if (successor != null) copyToRoot(successor).replacePredecessor(originRoot, joinTree);

      resolveUsedTree(rootOf(joinTree));
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

  public static Iterable<PlanNode> rearrangeJoin(
      InnerJoinNode originRoot, LeftJoin op, Interpretations inter) {
    final List<JoinNode> path = findPathToLeftJoin(originRoot);
    if (isEmpty(path)) return emptyList();

    final JoinNode joinTree = rebuildJoinTree(Lists.reverse(path), path.size() - 1);
    if (!isValidJoin(joinTree)) return emptyList();

    resolveUsedTree(joinTree);
    if (!isEligibleMatch(joinTree, op, inter)) return emptyList();

    final PlanNode successor = originRoot.successor();
    if (successor != null) copyToRoot(successor).replacePredecessor(originRoot, joinTree);

    resolveUsedTree(rootOf(joinTree));
    return singletonList(joinTree);
  }

  private static List<JoinNode> findPathToLeftJoin(PlanNode node) {
    if (!(node instanceof JoinNode)) return null;
    if (node instanceof LeftJoinNode) return newArrayList((JoinNode) node);
    assert node instanceof InnerJoinNode;
    final List<JoinNode> path = findPathToLeftJoin(node.predecessors()[0]);
    if (path != null) path.add((JoinNode) node);
    return path;
  }

  private static JoinNode rebuildJoinTree(List<? extends JoinNode> nodes, int rootIdx) {
    final boolean shiftLeaves = rootIdx >= nodes.size();
    rootIdx = Math.min(rootIdx, nodes.size() - 1);

    // use nodes[rootIdx] as root, construct a left-deep join tree with remaining nodes
    final JoinNode root = (JoinNode) nodes.get(rootIdx).copy();
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
    if (!(node instanceof JoinNode)) return true;
    final JoinNode join = (JoinNode) node;
    // check if all attributes are presented in input
    for (PlanAttribute attr : join.usedAttributes())
      if (join.resolveAttribute(attr) == null) return false;
    return isValidJoin(join.predecessors()[0]);
  }

  private static boolean isEligibleMatch(JoinNode node, Join op, Interpretations inter) {
    final AttributeInterpretation leftInter = inter.getAttributes(op.leftFields());
    final AttributeInterpretation rightInter = inter.getAttributes(op.rightFields());

    // check whether compatible with existing assignment
    if (leftInter != null && !leftInter.isCompatible(node.leftAttributes())) return false;
    if (rightInter != null && !rightInter.isCompatible(node.rightAttributes())) return false;

    // check whether the Reference constraint can be enforced
    if (inter.constraints().requiresReference(op.leftFields(), op.rightFields())) {
      final List<Column> referred = listMap(PlanAttribute::column, node.rightAttributes());
      for (PlanAttribute referee : node.leftAttributes()) {
        final Column column = referee.column();
        if (column == null || !column.references(referred)) return false;
      }
    }

    return true;
  }
}
