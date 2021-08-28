package sjtu.ipads.wtune.superopt;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sqlparser.plan.CombinedFilterNode;
import sjtu.ipads.wtune.sqlparser.plan.FilterNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan.SimpleFilterNode;
import sjtu.ipads.wtune.superopt.fragment.ConstraintAwareModel;
import sjtu.ipads.wtune.superopt.fragment.Filter;
import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.optimizer.ReversedMatch;
import sjtu.ipads.wtune.superopt.substitution.Substitution;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static sjtu.ipads.wtune.superopt.TestHelper.mkPlan;

@Tag("fast")
@Tag("optimizer")
public class TestFilterReversedMatch {
  @Test
  void testFullCover() {
    final PlanNode plan = mkPlan("Select a.i From a Where a.i = 1 And a.j < 10");
    final Fragment fragment = Fragment.parse("Proj(Filter(Proj(Input)))", null);
    final ReversedMatch<FilterNode, Filter> m = ReversedMatch.forFilter();
    final List<FilterNode> results =
        m.reverseMatch(
            (FilterNode) plan.predecessors()[0], (Filter) fragment.root().predecessors()[0], null);

    assertEquals(1, results.size());
    assertTrue(results.get(0) instanceof CombinedFilterNode);
  }

  @Test
  void test0() {
    final PlanNode plan = mkPlan("Select a.i From a Where a.i = 1 And a.j < 10 And a.k > 20");
    final Substitution sub =
        Substitution.parse(
            "Proj<a0>(Filter<p0 a1>(Filter<p1 a2>(Input<t0>)))|Input<t1>|AttrsEq(a0,a1)");
    final ConstraintAwareModel model = ConstraintAwareModel.mk(plan.context(), sub.constraints());
    sub._0().root().match(plan, model); // Assign to a0.

    final ReversedMatch<FilterNode, Filter> m = ReversedMatch.forFilter();
    final List<FilterNode> results =
        m.reverseMatch(
            (FilterNode) plan.predecessors()[0], (Filter) sub._0().root().predecessors()[0], model);

    assertEquals(1, results.size());
    final FilterNode result = results.get(0);
    assertTrue(result instanceof SimpleFilterNode);
    assertTrue(result.predecessors()[0] instanceof SimpleFilterNode);
  }

  @Test
  void test1() {
    final PlanNode plan = mkPlan("Select a.i From a Where a.i = 1 And a.j < 10 And a.k > 20");
    final Substitution sub =
        Substitution.parse(
            "Proj<a0>(Filter<p0 a1>(Filter<p1 a2>(Proj<a3>(Input<t0>))))|Input<t1>|AttrsEq(a0,a1)");
    final ConstraintAwareModel model = ConstraintAwareModel.mk(plan.context(), sub.constraints());
    sub._0().root().match(plan, model); // Assign to a0.

    final ReversedMatch<FilterNode, Filter> m = ReversedMatch.forFilter();
    final List<FilterNode> results =
        m.reverseMatch(
            (FilterNode) plan.predecessors()[0], (Filter) sub._0().root().predecessors()[0], model);

    assertEquals(1, results.size());
    final FilterNode result = results.get(0);
    System.out.println(result);
    assertTrue(result instanceof SimpleFilterNode);
    assertTrue(result.predecessors()[0] instanceof CombinedFilterNode);
  }

  @Test
  void test2() {
    final PlanNode plan = mkPlan("Select a.i From a Where a.i = 1 And a.i < 10 And a.i = 1");
    final Substitution sub =
        Substitution.parse(
            "Filter<p0 a0>(Filter<p1 a1>(Input<t0>))|Input<t1>|AttrsEq(a0,a1);PredicateEq(p0,p1)");
    final ConstraintAwareModel model = ConstraintAwareModel.mk(plan.context(), sub.constraints());

    final ReversedMatch<FilterNode, Filter> m = ReversedMatch.forFilter();
    final List<FilterNode> results =
        m.reverseMatch((FilterNode) plan.predecessors()[0], (Filter) sub._0().root(), model);

    assertEquals(1, results.size());
    final FilterNode result = results.get(0);
    assertTrue(result.predicate().isEquiCondition());
    assertTrue(((SimpleFilterNode) result.predecessors()[0]).predicate().isEquiCondition());
    assertFalse(
        ((SimpleFilterNode) result.predecessors()[0].predecessors()[0])
            .predicate()
            .isEquiCondition());
  }
}
