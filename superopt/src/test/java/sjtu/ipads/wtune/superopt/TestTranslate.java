package sjtu.ipads.wtune.superopt;

import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.superopt.plan.Plan;
import sjtu.ipads.wtune.superopt.plan.internal.ToASTTranslator;
import sjtu.ipads.wtune.superopt.util.PlaceholderNumbering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static sjtu.ipads.wtune.superopt.plan.Plan.wrap;
import static sjtu.ipads.wtune.superopt.plan.PlanNode.*;

public class TestTranslate {
  @Test
  void test() {
    final Plan plan =
        wrap(plainFilter(
                plainFilter(
                    innerJoin(
                        innerJoin(null, null),
                        subqueryFilter(
                            proj(null),
                            proj(leftJoin(plainFilter(null), subqueryFilter(null, null))))))))
            .setup();

    final PlaceholderNumbering numbering = PlaceholderNumbering.build();
    numbering.number(plan);

    final ToASTTranslator translator = ToASTTranslator.build();
    translator.setNumbering(numbering);

    assertEquals(
        "SELECT * FROM `t0` INNER JOIN `t1` ON `c4` = `c5` INNER JOIN (SELECT * FROM (SELECT `c7` FROM `t2`) WHERE `c6` IN (SELECT `c8` FROM (SELECT * FROM `t3` WHERE P2(`c11`)) LEFT JOIN (SELECT * FROM `t4` WHERE `c12` IN (SELECT * FROM `t5`)) ON `c9` = `c10`)) ON `c2` = `c3` WHERE P0(`c0`) AND P1(`c1`)",
        translator.translate(plan).toString());
  }
}
