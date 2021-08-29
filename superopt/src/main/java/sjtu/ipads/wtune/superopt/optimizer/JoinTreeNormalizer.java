package sjtu.ipads.wtune.superopt.optimizer;

import sjtu.ipads.wtune.sqlparser.plan.JoinNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanContext;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan.Value;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.TreeScaffold.displaceGlobal;
import static sjtu.ipads.wtune.common.utils.TreeScaffold.replaceLocal;

public class JoinTreeNormalizer {
  static JoinNode normalize(JoinNode joinRoot) {
    if (isLeftDeepJoinTree(joinRoot)) return joinRoot;
    else return ((JoinNode) displaceGlobal(joinRoot, normalize0(joinRoot), false));
  }

  private static PlanNode normalize0(PlanNode node) {
    if (!node.kind().isJoin()) return node;

    final PlanNode lhs = normalize0(node.predecessors()[0]);
    final PlanNode rhs = normalize0(node.predecessors()[1]);

    if (lhs == node.predecessors()[0] && rhs == node.predecessors()[1] && !rhs.kind().isJoin())
      return node; // No modification needed.

    if (!rhs.kind().isJoin()) return replaceLocal(node, lhs, rhs);

    final PlanContext oldCtx = node.context();
    final PlanNode a = lhs, b = rhs.predecessors()[0], c = rhs.predecessors()[1];
    final JoinNode join = ((JoinNode) node), newJoin = ((JoinNode) rhs);
    assert !c.kind().isJoin();

    final List<Value> rhsAttrs = oldCtx.deRef(join.rhsRefs());
    if (b.values().containsAll(rhsAttrs)) {
      // 1. join<a.x=b.y>(a,newJoin<b.z=c.w>(b,c)) => newJoin<b.z=c.w>(join<a.x=b.y>(a,b),c)
      final PlanNode newLhs = normalize0(replaceLocal(join, a, b));
      return replaceLocal(newJoin, newLhs, c);
    } else {
      // 2. join<a.x=c.y>(a,newJoin<b.z=c.w>(b,c)) => newJoin<b.z=c.w>(join<a.x=c.y>(a,c),b)
      final PlanNode newLhs = replaceLocal(join, a, c);
      return normalize0(((JoinNode) replaceLocal(newJoin, newLhs, b)).flip(null));
    }
  }

  private static boolean isLeftDeepJoinTree(PlanNode n) {
    return !n.kind().isJoin()
        || (!n.predecessors()[1].kind().isJoin() && isLeftDeepJoinTree(n.predecessors()[0]));
  }
}
