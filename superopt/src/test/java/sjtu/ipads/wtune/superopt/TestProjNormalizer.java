package sjtu.ipads.wtune.superopt;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static sjtu.ipads.wtune.common.utils.FuncUtils.zipForEach;
import static sjtu.ipads.wtune.common.utils.TreeNode.treeRootOf;
import static sjtu.ipads.wtune.common.utils.TreeScaffold.displaceGlobal;
import static sjtu.ipads.wtune.sqlparser.plan.PlanSupport.disambiguate;
import static sjtu.ipads.wtune.sqlparser.plan.PlanSupport.translateAsAst;
import static sjtu.ipads.wtune.superopt.TestHelper.mkPlan;
import static sjtu.ipads.wtune.superopt.optimizer.OptimizerSupport.insertProjIfNeed;
import static sjtu.ipads.wtune.superopt.optimizer.OptimizerSupport.removeProjIfNeed;

@Tag("fast")
@Tag("optimizer")
public class TestProjNormalizer {
  @Test
  void testRemove() {
    final PlanNode planNode = mkPlan("Select sub.i From (Select * From a) As sub");
    assertSame(planNode, removeProjIfNeed(planNode));

    final PlanNode newNode = removeProjIfNeed(planNode.predecessors()[0]);
    assertEquals(OperatorType.INPUT, newNode.kind());
    assertEquals("SELECT `a`.`i` AS `i` FROM `a` AS `a`", translateAsAst(treeRootOf(newNode)).toString());
  }

  @Test
  void testInsert() {
    final PlanNode planNode =
        mkPlan("Select sub.i From b Join (Select * From a Where a.i = 1) As sub On b.x = sub.i");
    final PlanNode toDelete = planNode.predecessors()[0].predecessors()[1];
    final PlanNode filter = displaceGlobal(toDelete, toDelete.predecessors()[0], true);
    zipForEach(toDelete.values(), filter.values(), filter.context()::replaceValue);

    final PlanNode newNode = insertProjIfNeed(filter);
    final PlanNode newPlan = treeRootOf(newNode);
    assertEquals(OperatorType.PROJ, newNode.kind());
    assertEquals(
        "SELECT `sub_0`.`i` AS `i` FROM `b` AS `b` INNER JOIN (SELECT `a`.`i` AS `i`, `a`.`j` AS `j`, `a`.`k` AS `k` FROM `a` AS `a` WHERE `a`.`i` = 1) AS `sub_0` ON `b`.`x` = `sub_0`.`i`",
        translateAsAst(disambiguate(newPlan)).toString());
  }
}
