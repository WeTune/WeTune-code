package sjtu.ipads.wtune.superopt;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan1.JoinNode;
import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;
import sjtu.ipads.wtune.stmt.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static sjtu.ipads.wtune.common.utils.TreeNode.treeRootOf;
import static sjtu.ipads.wtune.sqlparser.plan1.PlanSupport.assemblePlan;
import static sjtu.ipads.wtune.sqlparser.plan1.PlanSupport.translateAsAst;
import static sjtu.ipads.wtune.superopt.TestHelper.mkJoin;
import static sjtu.ipads.wtune.superopt.optimizer1.OptimizerSupport.normalizeJoinTree;

@Tag("fast")
@Tag("optimizer")
public class TestJoinTreeNormalization {
  @Test
  void test() {
    final JoinNode joinNode = mkJoin("a Join (b Join (c Join d On c.v=d.q) On b.y=d.p) On a.i=b.x");
    final PlanNode plan = treeRootOf(normalizeJoinTree(joinNode));
    assertEquals(
        "SELECT `a`.`i` FROM `a` AS `a` INNER JOIN `b` AS `b` ON `a`.`i` = `b`.`x` INNER JOIN `d` AS `d` ON `b`.`y` = `d`.`p` INNER JOIN `c` AS `c` ON `c`.`v` = `d`.`q`",
        translateAsAst(plan).toString());
  }
}
