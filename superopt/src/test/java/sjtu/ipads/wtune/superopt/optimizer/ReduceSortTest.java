package sjtu.ipads.wtune.superopt.optimizer;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sqlparser.plan.PlanContext;
import sjtu.ipads.wtune.sqlparser.plan.PlanSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sjtu.ipads.wtune.superopt.TestHelper.parsePlan;

@Tag("optimizer")
@Tag("fast")
public class ReduceSortTest {
  @Test
  void test() {
    final PlanContext plan =
        parsePlan(
            "Select sub0.i From (Select a.i From a Order By a.j) sub0 "
                + "Join (Select a.i From a Order By a.j Limit 1) sub1 On sub0.i = sub1.i "
                + "Where sub0.i In (Select a.i From a Order By a.j) "
                + "And sub0.i In (Select a.i From a Order By a.j Limit 1)");

    final int oldRoot = plan.root();
    final int newRoot = new ReduceSort(plan).reduce(oldRoot);
    assertEquals(oldRoot, newRoot);

    assertEquals(
        "SELECT `sub0`.`i` AS `i` "
            + "FROM (SELECT `a`.`i` AS `i` FROM `a` AS `a`) AS `sub0` "
            + "INNER JOIN (SELECT `q0`.`i` AS `i` FROM `a` AS `q0` ORDER BY `q0`.`j` LIMIT 1) AS `sub1` "
            + "ON `sub0`.`i` = `sub1`.`i` "
            + "WHERE `sub0`.`i` IN (SELECT `q1`.`i` AS `i` FROM `a` AS `q1`) "
            + "AND `sub0`.`i` IN (SELECT `q2`.`i` AS `i` FROM `a` AS `q2` LIMIT 1)",
        PlanSupport.translateAsAst(plan, newRoot, false).toString());
  }
}
