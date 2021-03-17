package sjtu.ipads.wtune.superopt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static sjtu.ipads.wtune.superopt.fragment.Fragment.wrap;
import static sjtu.ipads.wtune.superopt.fragment.Operator.innerJoin;
import static sjtu.ipads.wtune.superopt.fragment.Operator.leftJoin;
import static sjtu.ipads.wtune.superopt.fragment.Operator.plainFilter;
import static sjtu.ipads.wtune.superopt.fragment.Operator.proj;
import static sjtu.ipads.wtune.superopt.fragment.Operator.subqueryFilter;

import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.fragment.ToASTTranslator;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Numbering;

public class TestTranslate {
  @Test
  void test() {
    final Fragment fragment =
        wrap(plainFilter(
                plainFilter(
                    innerJoin(
                        innerJoin(null, null),
                        subqueryFilter(
                            proj(null),
                            proj(leftJoin(plainFilter(null), subqueryFilter(null, null))))))))
            .setup();

    final Numbering numbering = Numbering.make();
    numbering.number(fragment);

    final ToASTTranslator translator = ToASTTranslator.build();
    translator.setNumbering(numbering);

    assertEquals(
        "SELECT * FROM `t0` INNER JOIN `t1` ON `c4` = `c5` INNER JOIN (SELECT * FROM (SELECT `c7` FROM `t2`) WHERE `c6` IN (SELECT `c8` FROM (SELECT * FROM `t3` WHERE P2(`c11`)) LEFT JOIN (SELECT * FROM `t4` WHERE `c12` IN (SELECT * FROM `t5`)) ON `c9` = `c10`)) ON `c2` = `c3` WHERE P0(`c0`) AND P1(`c1`)",
        translator.translate(fragment).toString());
  }
}
