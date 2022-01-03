package sjtu.ipads.wtune.superopt.optimizer;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sql.plan.PlanContext;
import sjtu.ipads.wtune.sql.plan.PlanSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static sjtu.ipads.wtune.superopt.TestHelper.parsePlan;

@Tag("fast")
@Tag("optimizer")
public class NormalizeJoinTest {
  @Test
  void test() {
    final PlanContext plan =
        parsePlan("Select a.i From a Join (b Join (c Join d On c.v=d.q) On b.y=d.p) On a.i=b.x");
    new NormalizeJoin(plan).normalizeTree(plan.root());
    assertEquals(
        "SELECT `a`.`i` AS `i` FROM `a` AS `a` INNER JOIN `b` AS `b` ON `a`.`i` = `b`.`x` INNER JOIN `d` AS `d` ON `b`.`y` = `d`.`p` INNER JOIN `c` AS `c` ON `c`.`v` = `d`.`q`",
        PlanSupport.translateAsAst(plan, plan.root(), false).toString());
  }
}
