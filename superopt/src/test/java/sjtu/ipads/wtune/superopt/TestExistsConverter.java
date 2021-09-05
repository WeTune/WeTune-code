package sjtu.ipads.wtune.superopt;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static sjtu.ipads.wtune.common.utils.TreeNode.treeRootOf;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.SIMPLE_FILTER;
import static sjtu.ipads.wtune.sqlparser.plan.PlanSupport.translateAsAst;
import static sjtu.ipads.wtune.superopt.TestHelper.mkPlan;
import static sjtu.ipads.wtune.superopt.optimizer.OptimizerSupport.convertExistsFilterIfNeed;

@Tag("fast")
@Tag("optimizer")
public class TestExistsConverter {
  @Test
  void test() {
    final PlanNode planNode =
        mkPlan("Select a.i From a Where Exists (Select 1 From b Where a.i = b.x)");

    final PlanNode newFilter = convertExistsFilterIfNeed(planNode.predecessors()[0]);
    assertEquals(SIMPLE_FILTER, newFilter.kind());
    assertEquals(1, newFilter.refs().size());
    assertEquals(
        "SELECT `a`.`i` AS `i` FROM `a` AS `a` WHERE EXISTS (SELECT 1 FROM `b` AS `b` WHERE `a`.`i` = `b`.`x`)",
        translateAsAst(treeRootOf(newFilter)).toString());
  }
}
