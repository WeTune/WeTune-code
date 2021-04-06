package sjtu.ipads.wtune.superopt.optimizer.join;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.LeftJoin;
import static sjtu.ipads.wtune.sqlparser.plan.PlanNode.copyToRoot;

import java.util.ArrayList;
import java.util.List;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDef;
import sjtu.ipads.wtune.sqlparser.plan.JoinNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.schema.Column;
import sjtu.ipads.wtune.superopt.fragment.Join;
import sjtu.ipads.wtune.superopt.fragment.symbolic.AttributeInterpretation;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Interpretations;

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
    //    resolveUsedToRoot(successor); // necessary?
    return newTree;
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
