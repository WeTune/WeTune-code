package sjtu.ipads.wtune.superopt.optimization.internal;

import sjtu.ipads.wtune.sqlparser.plan.AttributeDef;
import sjtu.ipads.wtune.sqlparser.plan.JoinNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.schema.Column;
import sjtu.ipads.wtune.superopt.fragment.Join;
import sjtu.ipads.wtune.superopt.fragment.symbolic.AttributeInterpretation;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Interpretations;

import java.util.ArrayList;
import java.util.List;

import static sjtu.ipads.wtune.common.utils.Commons.tail;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.LeftJoin;
import static sjtu.ipads.wtune.sqlparser.plan.PlanNode.*;

public class JoinHint {
  public static Iterable<PlanNode> rearrangeJoinNew(
      JoinNode joinRoot, Join op, Interpretations inter) {
    final JoinTree tree = JoinTree.make(joinRoot);
    final List<PlanNode> joinTrees = new ArrayList<>(tree.size() + 1);

    for (int i = tree.size() - 1; i >= -1; i--) {
      final PlanNode newTree = tryWithRoot(tree, i, op, inter);
      if (newTree != null) joinTrees.add(newTree);
    }

    return joinTrees;
  }

  private static PlanNode tryWithRoot(JoinTree tree, int rootIdx, Join op, Interpretations inter) {
    if (!tree.isValidRoot(rootIdx)) return null;
    if (!isEligibleMatch(tree.get(Math.max(rootIdx, 0)), op, inter, rootIdx == -1)) return null;

    final JoinNode newTree = tree.withRoot(rootIdx).copyAndRebuild();
    final PlanNode successor = copyToRoot(tree.originalRoot().successor());
    successor.replacePredecessor(tree.originalRoot(), newTree);
    resolveUsedToRoot(successor);
    return newTree;
  }

  public static Iterable<PlanNode> rearrangeJoin(
      JoinNode joinRoot, Join op, Interpretations inter) {
    // premise: the join tree is always kept left-deep
    // This function tries to shift each join node in join tree as the root
    final List<JoinNode> joins = gatherJoins(joinRoot);
    final List<PlanNode> joinTrees = new ArrayList<>(joins.size() + 1);
    final PlanNode successor = joinRoot.successor();
    assert successor != null;

    for (int i = 0, bound = joins.size(); i <= bound; i++) {
      if (i < bound && joins.get(i).type() != op.type()) continue;

      final JoinNode joinTree = rebuildJoinTree(joins, i);
      if (!isValidJoinTree(joinTree)) continue;
      if (!isEligibleMatch(joinTree, op, inter)) continue;

      final PlanNode newSuccessor = copyToRoot(successor);
      newSuccessor.replacePredecessor(joinRoot, joinTree);
      resolveUsedToRoot(newSuccessor);

      joinTrees.add(joinTree);
    }

    return joinTrees;
  }

  private static List<JoinNode> gatherJoins(PlanNode node) {
    final List<JoinNode> joins = new ArrayList<>();

    while (node.type().isJoin()) {
      assert !node.predecessors()[1].type().isJoin();
      joins.add((JoinNode) node);
      node = node.predecessors()[0];
    }

    return joins;
  }

  private static JoinNode rebuildJoinTree(List<? extends JoinNode> nodes, int rootIdx) {
    // use `rootIdx` >= node.size to indicate swapping left-most leaf to right additionally
    // e.g. Join1(Join2(a,b),c) -> Join2(Join1(a,c),b) -> Join2(Join1(b,c),a)
    final boolean requireFlip = rootIdx >= nodes.size();
    rootIdx = Math.min(rootIdx, nodes.size() - 1);

    // use nodes[rootIdx] as root, construct a left-deep join tree with remaining nodes
    final JoinNode root = (JoinNode) nodes.get(rootIdx).copy();
    if (requireFlip && root.type() == LeftJoin) return null; // left join shouldn't be flipped

    root.setPredecessor(1, copyOnTree(root.predecessors()[1]));

    PlanNode successor = root;
    for (int i = 0, bound = nodes.size(); i < bound; i++) {
      if (i == rootIdx) continue;

      final PlanNode current = nodes.get(i).copy();
      current.setPredecessor(1, copyOnTree(current.predecessors()[1]));
      successor.setPredecessor(0, current);
      successor = current;
    }
    successor.setPredecessor(0, copyOnTree(tail(nodes).predecessors()[0]));

    if (requireFlip) {
      final PlanNode rightMostLeaf = root.predecessors()[1];
      final PlanNode leftMostLeaf = successor.predecessors()[0];
      root.setPredecessor(1, leftMostLeaf);
      successor.setPredecessor(0, rightMostLeaf);
    }

    return root;
  }

  private static boolean isValidJoinTree(PlanNode node) {
    if (node == null) return false;
    if (!(node instanceof JoinNode)) return true;
    final JoinNode join = (JoinNode) node;
    // check if all attributes are presented in input
    for (AttributeDef attr : join.usedAttributes())
      if (join.resolveAttribute(attr) == null) return false;
    return isValidJoinTree(join.predecessors()[0]);
  }

  private static boolean isEligibleMatch(JoinNode node, Join op, Interpretations inter) {
    resolveUsedOnTree(node);

    final AttributeInterpretation leftInter = inter.getAttributes(op.leftFields());
    final AttributeInterpretation rightInter = inter.getAttributes(op.rightFields());

    // check whether compatible with existing assignment
    if (leftInter != null && !leftInter.isCompatible(node.leftAttributes())) return false;
    if (rightInter != null && !rightInter.isCompatible(node.rightAttributes())) return false;

    // check whether the Reference constraint can be enforced
    if (inter.constraints().requiresReference(op.leftFields(), op.rightFields())) {
      final List<Column> referred = listMap(AttributeDef::referredColumn, node.rightAttributes());
      for (AttributeDef referee : node.leftAttributes()) {
        final Column column = referee.referredColumn();
        if (column == null || !column.references(referred)) return false;
      }
    }

    return true;
  }

  private static boolean isEligibleMatch(
      JoinNode node, Join op, Interpretations inter, boolean swapLeaf) {
    if (node.type() != op.type()) return false;
    if (node.type() == LeftJoin && swapLeaf) return false;

    final AttributeInterpretation leftInter = inter.getAttributes(op.leftFields());
    final AttributeInterpretation rightInter = inter.getAttributes(op.rightFields());
    final List<AttributeDef> leftAttrs = swapLeaf ? node.rightAttributes() : node.leftAttributes();
    final List<AttributeDef> rightAttrs = swapLeaf ? node.leftAttributes() : node.rightAttributes();

    // check whether compatible with existing assignment
    if (leftInter != null && !leftInter.isCompatible(leftAttrs)) return false;
    if (rightInter != null && !rightInter.isCompatible(rightAttrs)) return false;

    // check whether the Reference constraint can be enforced
    if (inter.constraints().requiresReference(op.leftFields(), op.rightFields())) {
      final List<Column> referred = listMap(AttributeDef::referredColumn, rightAttrs);
      for (AttributeDef referee : leftAttrs) {
        final Column column = referee.referredColumn();
        if (column == null || !column.references(referred)) return false;
      }
    }

    return true;
  }
}
