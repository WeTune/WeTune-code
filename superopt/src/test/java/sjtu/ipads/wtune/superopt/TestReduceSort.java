package sjtu.ipads.wtune.superopt;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;
import sjtu.ipads.wtune.superopt.optimizer1.OptimizerSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static sjtu.ipads.wtune.sqlparser.plan1.PlanSupport.translateAsAst;
import static sjtu.ipads.wtune.superopt.TestHelper.mkPlan;

@Tag("optimizer")
@Tag("fast")
public class TestReduceSort {
  @Test
  void test() {
    final PlanNode plan =
        mkPlan(
            "Select sub0.i From (Select a.i From a Order By a.j) sub0 "
                + "Join (Select a.i From a Order By a.j Limit 1) sub1 On sub0.i = sub1.i "
                + "Where sub0.i In (Select a.i From a Order By a.j) "
                + "And sub0.i In (Select a.i From a Order By a.j Limit 1)");
    final PlanNode newPlan = OptimizerSupport.reduceSort(plan);
    assertEquals(
        "SELECT `sub0`.`i` "
            + "FROM (SELECT `a`.`i` FROM `a` AS `a`) AS `sub0` "
            + "INNER JOIN (SELECT `a`.`i` FROM `a` AS `a` ORDER BY `a`.`j` LIMIT 1 OFFSET 1) AS `sub1` "
            + "ON `sub0`.`i` = `sub1`.`i` "
            + "WHERE `sub0`.`i` IN (SELECT `a`.`i` FROM `a` AS `a` LIMIT 1 OFFSET 1) "
            + "AND `sub0`.`i` IN (SELECT `a`.`i` FROM `a` AS `a`)",
        translateAsAst(newPlan).toString());
  }
}
