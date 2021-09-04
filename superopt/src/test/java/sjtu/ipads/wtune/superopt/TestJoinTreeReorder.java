package sjtu.ipads.wtune.superopt;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sqlparser.plan.JoinNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.superopt.optimizer.LinearJoinTree;

import static org.junit.jupiter.api.Assertions.*;
import static sjtu.ipads.wtune.common.utils.TreeNode.treeRootOf;
import static sjtu.ipads.wtune.sqlparser.plan.PlanSupport.translateAsAst;
import static sjtu.ipads.wtune.superopt.TestHelper.mkJoin;
import static sjtu.ipads.wtune.superopt.optimizer.OptimizerSupport.linearizeJoinTree;

@Tag("fast")
@Tag("optimizer")
public class TestJoinTreeReorder {
  @Test
  void testReorder0() {
    final JoinNode join = mkJoin("a Join b On a.i = b.x Join c On b.y = c.u Join d On b.z = d.p");
    final LinearJoinTree linearJoinTree = linearizeJoinTree(join);
    assertTrue(linearJoinTree.isEligibleRoot(0));
    assertFalse(linearJoinTree.isEligibleRoot(1));
    assertTrue(linearJoinTree.isEligibleRoot(2));
    assertTrue(linearJoinTree.isEligibleRoot(3));
    final PlanNode newPlan0 = treeRootOf(linearJoinTree.mkRootedByJoinee(0));
    final PlanNode newPlan1 = treeRootOf(linearJoinTree.mkRootedByJoinee(2));
    final PlanNode newPlan2 = treeRootOf(linearJoinTree.mkRootedByJoinee(3));

    assertEquals(
        "SELECT `a`.`i` AS `i` FROM `b` AS `b` INNER JOIN `c` AS `c` ON `b`.`y` = `c`.`u` INNER JOIN `d` AS `d` ON `b`.`z` = `d`.`p` INNER JOIN `a` AS `a` ON `a`.`i` = `b`.`x`",
        translateAsAst(newPlan0).toString());
    assertEquals(
        "SELECT `a`.`i` AS `i` FROM `a` AS `a` INNER JOIN `b` AS `b` ON `a`.`i` = `b`.`x` INNER JOIN `d` AS `d` ON `b`.`z` = `d`.`p` INNER JOIN `c` AS `c` ON `b`.`y` = `c`.`u`",
        translateAsAst(newPlan1).toString());
    assertEquals(
        "SELECT `a`.`i` AS `i` FROM `a` AS `a` INNER JOIN `b` AS `b` ON `a`.`i` = `b`.`x` INNER JOIN `c` AS `c` ON `b`.`y` = `c`.`u` INNER JOIN `d` AS `d` ON `b`.`z` = `d`.`p`",
        translateAsAst(newPlan2).toString());
  }

  @Test
  void testReorder1() {
    final JoinNode join =
        mkJoin("a Join b On a.i = b.x Left Join c On b.y = c.u Join d On b.z = d.p");
    final LinearJoinTree linearJoinTree = linearizeJoinTree(join);
    assertTrue(linearJoinTree.isEligibleRoot(0));
    assertFalse(linearJoinTree.isEligibleRoot(1));
    assertTrue(linearJoinTree.isEligibleRoot(2));
    assertTrue(linearJoinTree.isEligibleRoot(3));

    assertEquals(
        "SELECT * FROM `a` AS `a` INNER JOIN `b` AS `b` ON `a`.`i` = `b`.`x` INNER JOIN `d` AS `d` ON `b`.`z` = `d`.`p` LEFT JOIN `c` AS `c` ON `b`.`y` = `c`.`u`",
        translateAsAst(linearJoinTree.mkRootedByJoinee(2)).toString());
    assertEquals(
        "SELECT * FROM `a` AS `a` INNER JOIN `b` AS `b` ON `a`.`i` = `b`.`x` LEFT JOIN `c` AS `c` ON `b`.`y` = `c`.`u` INNER JOIN `d` AS `d` ON `b`.`z` = `d`.`p`",
        translateAsAst(linearJoinTree.mkRootedByJoinee(3)).toString());
  }
}
