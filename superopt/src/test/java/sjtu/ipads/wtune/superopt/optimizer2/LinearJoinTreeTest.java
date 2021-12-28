package sjtu.ipads.wtune.superopt.optimizer2;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sqlparser.plan1.PlanContext;
import sjtu.ipads.wtune.sqlparser.plan1.PlanSupport;
import sjtu.ipads.wtune.superopt.TestHelper;

import static org.junit.jupiter.api.Assertions.*;

@Tag("fast")
@Tag("optimizer")
public class LinearJoinTreeTest {

  private static class LinearJoinTreeHelper {
    private PlanContext ctx;
    private LinearJoinTree tree;

    LinearJoinTree mkJoinTree(String joinSnippet) {
      this.ctx = TestHelper.parsePlan("Select a.i From " + joinSnippet);
      this.tree = LinearJoinTree.linearize(ctx, ctx.childOf(ctx.root(), 0));
      return tree;
    }

    PlanContext plan() {
      return ctx;
    }

    int treeRoot() {
      return ctx.childOf(ctx.root(), 0);
    }

    String mkSqlRootedBy(int i) {
      final PlanContext newPlan = tree.mkRootedBy(i);
      return PlanSupport.translateAsAst(newPlan, newPlan.root(), false).toString();
    }
  }

  private final LinearJoinTreeHelper helper = new LinearJoinTreeHelper();

  @Test
  void testReorder0() {
    final String joinSql = "a Join b On a.i = b.x Join c On b.y = c.u Join d On b.z = d.p";
    final LinearJoinTree joinTree = helper.mkJoinTree(joinSql);
    assertEquals(3, joinTree.numJoiners());
    assertTrue(joinTree.isEligibleRoot(-1));
    assertFalse(joinTree.isEligibleRoot(0));
    assertTrue(joinTree.isEligibleRoot(1));
    assertTrue(joinTree.isEligibleRoot(2));

    assertEquals(
        "SELECT `a`.`i` AS `i` FROM `b` AS `b` INNER JOIN `c` AS `c` ON `b`.`y` = `c`.`u` INNER JOIN `d` AS `d` ON `b`.`z` = `d`.`p` INNER JOIN `a` AS `a` ON `a`.`i` = `b`.`x`",
        helper.mkSqlRootedBy(-1));
    assertEquals(
        "SELECT `a`.`i` AS `i` FROM `a` AS `a` INNER JOIN `b` AS `b` ON `a`.`i` = `b`.`x` INNER JOIN `d` AS `d` ON `b`.`z` = `d`.`p` INNER JOIN `c` AS `c` ON `b`.`y` = `c`.`u`",
        helper.mkSqlRootedBy(1));
    assertEquals(
        "SELECT `a`.`i` AS `i` FROM `a` AS `a` INNER JOIN `b` AS `b` ON `a`.`i` = `b`.`x` INNER JOIN `c` AS `c` ON `b`.`y` = `c`.`u` INNER JOIN `d` AS `d` ON `b`.`z` = `d`.`p`",
        helper.mkSqlRootedBy(2));
  }

  @Test
  void testReorder1() {
    final String joinSql = "a Join b On a.i = b.x Left Join c On b.y = c.u Join d On b.z = d.p";
    final LinearJoinTree joinTree = helper.mkJoinTree(joinSql);
    assertTrue(joinTree.isEligibleRoot(-1));
    assertFalse(joinTree.isEligibleRoot(0));
    assertTrue(joinTree.isEligibleRoot(1));
    assertTrue(joinTree.isEligibleRoot(2));

    assertEquals(
        "SELECT `a`.`i` AS `i` FROM `a` AS `a` INNER JOIN `b` AS `b` ON `a`.`i` = `b`.`x` INNER JOIN `d` AS `d` ON `b`.`z` = `d`.`p` LEFT JOIN `c` AS `c` ON `b`.`y` = `c`.`u`",
        helper.mkSqlRootedBy(1));
    assertEquals(
        "SELECT `a`.`i` AS `i` FROM `a` AS `a` INNER JOIN `b` AS `b` ON `a`.`i` = `b`.`x` LEFT JOIN `c` AS `c` ON `b`.`y` = `c`.`u` INNER JOIN `d` AS `d` ON `b`.`z` = `d`.`p`",
        helper.mkSqlRootedBy(2));
  }
}
