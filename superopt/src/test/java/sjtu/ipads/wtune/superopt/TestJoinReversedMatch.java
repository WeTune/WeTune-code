package sjtu.ipads.wtune.superopt;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sqlparser.plan.JoinNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.superopt.fragment.ConstraintAwareModel;
import sjtu.ipads.wtune.superopt.fragment.Join;
import sjtu.ipads.wtune.superopt.optimizer.ReversedMatch;
import sjtu.ipads.wtune.superopt.substitution.Substitution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static sjtu.ipads.wtune.common.utils.TreeNode.treeRootOf;
import static sjtu.ipads.wtune.sqlparser.plan.PlanSupport.translateAsAst;
import static sjtu.ipads.wtune.superopt.TestHelper.mkJoin;

@Tag("fast")
@Tag("optimizer")
public class TestJoinReversedMatch {
  @Test
  void test() {
    final ReversedMatch<JoinNode, Join> r = ReversedMatch.forJoin();
    final Substitution substitution =
        Substitution.parse(
            "InnerJoin<a0 a1>(Input<t0>,Input<t1>)|LeftJoin<a2 a3>(Input<t2>,Input<t3>)|"
                + "TableEq(t0,t2);TableEq(t1,t3);AttrsEq(a0,a2);AttrsEq(a1,a3);"
                + "AttrsSub(a0,t0);AttrsSub(a1,t1);AttrsSub(a2,t2);AttrsSub(a3,t3);"
                + "NotNull(t0,a0);NotNull(t2,a2);"
                + "Reference(t0,a0,t1,a1);Reference(t2,a2,t3,a3)");

    final JoinNode join0 = mkJoin("d Join c On d.p=c.u Join a On d.q=a.i");
    final ConstraintAwareModel model0 =
        ConstraintAwareModel.mk(join0.context(), substitution.constraints());
    final PlanNode plan0 =
        treeRootOf(r.reverseMatch(join0, (Join) substitution._0().root(), model0).get(0));
    assertEquals(
        "SELECT `a`.`i` FROM `d` AS `d` INNER JOIN `a` AS `a` ON `d`.`q` = `a`.`i` INNER JOIN `c` AS `c` ON `d`.`p` = `c`.`u`",
        translateAsAst(plan0).toString());

    final JoinNode join1 = mkJoin("c Join d On d.p=c.u Join a On d.q=a.i");
    final ConstraintAwareModel model1 =
        ConstraintAwareModel.mk(join1.context(), substitution.constraints());
    final PlanNode plan1 =
        treeRootOf(r.reverseMatch(join1, (Join) substitution._0().root(), model1).get(0));
    assertEquals(
        "SELECT `a`.`i` FROM `d` AS `d` INNER JOIN `a` AS `a` ON `d`.`q` = `a`.`i` INNER JOIN `c` AS `c` ON `d`.`p` = `c`.`u`",
        translateAsAst(plan1).toString());
  }
}
