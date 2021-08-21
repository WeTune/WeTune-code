package sjtu.ipads.wtune.superopt;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan1.JoinNode;
import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;
import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.superopt.optimizer1.LinearJoinTree;

import static org.junit.jupiter.api.Assertions.*;
import static sjtu.ipads.wtune.common.utils.TreeNode.treeRootOf;
import static sjtu.ipads.wtune.sqlparser.plan1.PlanSupport.assemblePlan;
import static sjtu.ipads.wtune.sqlparser.plan1.PlanSupport.translateAsAst;
import static sjtu.ipads.wtune.superopt.optimizer1.OptimizerSupport.linearizeJoinTree;

@Tag("fast")
@Tag("optimizer")
public class TestJoinTreeReorder {
  private static JoinNode mkJoin(String joinTree) {
    final String sql = "Select a.i From " + joinTree;
    final Statement stmt = Statement.mk("test", sql, null);
    final ASTNode ast = stmt.parsed();
    final PlanNode plan = assemblePlan(ast, stmt.app().schema("base"));
    return ((JoinNode) plan.predecessors()[0]);
  }

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
        "SELECT `a`.`i` FROM `b` AS `b` INNER JOIN `c` AS `c` ON `b`.`y` = `c`.`u` INNER JOIN `d` AS `d` ON `b`.`z` = `d`.`p` INNER JOIN `a` AS `a` ON `a`.`i` = `b`.`x`",
        translateAsAst(newPlan0).toString());
    assertEquals(
        "SELECT `a`.`i` FROM `a` AS `a` INNER JOIN `b` AS `b` ON `a`.`i` = `b`.`x` INNER JOIN `d` AS `d` ON `b`.`z` = `d`.`p` INNER JOIN `c` AS `c` ON `b`.`y` = `c`.`u`",
        translateAsAst(newPlan1).toString());
    assertEquals(
        "SELECT `a`.`i` FROM `a` AS `a` INNER JOIN `b` AS `b` ON `a`.`i` = `b`.`x` INNER JOIN `c` AS `c` ON `b`.`y` = `c`.`u` INNER JOIN `d` AS `d` ON `b`.`z` = `d`.`p`",
        translateAsAst(newPlan2).toString());
  }

  @Test
  void testReorder1() {
    final JoinNode join =
        mkJoin("a Join b On a.i = b.x Left Join c On b.y = c.u Join d On b.z = d.p");
    final LinearJoinTree linearJoinTree = linearizeJoinTree(join);
    assertFalse(linearJoinTree.isEligibleRoot(0));
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
