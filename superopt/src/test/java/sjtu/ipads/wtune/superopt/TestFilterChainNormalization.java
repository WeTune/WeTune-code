package sjtu.ipads.wtune.superopt;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.common.utils.TreeScaffold;
import sjtu.ipads.wtune.sqlparser.plan.CombinedFilterNode;
import sjtu.ipads.wtune.sqlparser.plan.FilterNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan.SimpleFilterNode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sjtu.ipads.wtune.common.utils.TreeNode.treeRootOf;
import static sjtu.ipads.wtune.sqlparser.plan.PlanSupport.translateAsAst;
import static sjtu.ipads.wtune.superopt.TestHelper.mkPlan;
import static sjtu.ipads.wtune.superopt.optimizer.OptimizerSupport.normalizeFilterChain;

@Tag("fast")
@Tag("optimizer")
public class TestFilterChainNormalization {
  @Test
  void test() {
    final PlanNode plan = mkPlan("Select a.i From a Where a.i = 0 And a.j < 10");
    final FilterNode filter0 = (FilterNode) plan.predecessors()[0];
    final FilterNode filter1 = (FilterNode) plan.predecessors()[0].predecessors()[0];
    final PlanNode input = plan.predecessors()[0].predecessors()[0].predecessors()[0];

    final CombinedFilterNode combined = CombinedFilterNode.mk(List.of(filter0, filter1));
    final var scaffold = new TreeScaffold<>(plan);
    scaffold.rootTemplate().bindJointPoint(filter0, combined).bindJointPoint(combined, 0, input);
    final PlanNode planToTest = scaffold.instantiate();

    final FilterNode newFilter = normalizeFilterChain((FilterNode) planToTest.predecessors()[0]);
    final PlanNode newPlan = treeRootOf(newFilter);
    assertTrue(newFilter instanceof SimpleFilterNode);
    assertTrue(newFilter.predecessors()[0] instanceof SimpleFilterNode);
    assertEquals(
        "SELECT `a`.`i` AS `i` FROM `a` AS `a` WHERE `a`.`j` < 10 AND `a`.`i` = 0",
        translateAsAst(newPlan).toString());
  }
}
