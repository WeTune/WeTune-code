package sjtu.ipads.wtune.superopt;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static sjtu.ipads.wtune.sqlparser.plan.PlanSupport.translateAsAst;
import static sjtu.ipads.wtune.superopt.TestHelper.mkPlan;
import static sjtu.ipads.wtune.superopt.optimizer.OptimizerSupport.inferenceInnerJoin;

@Tag("fast")
@Tag("optimizer")
public class TestInnerJoinInference {
  @Test
  void test0() {
    final PlanNode plan =
        mkPlan("Select a.i From a Left Join b On a.i=b.x Left Join c On c.u=b.y Join d On d.p=c.v");
    final PlanNode newPlan = inferenceInnerJoin(plan);
    assertEquals(
        "SELECT `a`.`i` AS `i` FROM `a` AS `a` INNER JOIN `b` AS `b` ON `a`.`i` = `b`.`x` INNER JOIN `c` AS `c` ON `b`.`y` = `c`.`u` INNER JOIN `d` AS `d` ON `c`.`v` = `d`.`p`",
        translateAsAst(newPlan).toString());
  }

  @Test
  void test1() {
    final PlanNode plan =
        mkPlan("Select a.i From a Left Join b On a.i=b.x Left Join c On c.u=b.y Where c.v=0");
    final PlanNode newPlan = inferenceInnerJoin(plan);
    assertEquals(
        "SELECT `a`.`i` AS `i` FROM `a` AS `a` INNER JOIN `b` AS `b` ON `a`.`i` = `b`.`x` INNER JOIN `c` AS `c` ON `b`.`y` = `c`.`u` WHERE `c`.`v` = 0",
        translateAsAst(newPlan).toString());
  }
}
