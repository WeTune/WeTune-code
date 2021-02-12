package sjtu.ipads.wtune.superopt;

import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.superopt.plan.Plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static sjtu.ipads.wtune.superopt.internal.Canonicalize.canonicalize;
import static sjtu.ipads.wtune.superopt.plan.Plan.wrap;
import static sjtu.ipads.wtune.superopt.plan.PlanNode.plainFilter;
import static sjtu.ipads.wtune.superopt.plan.PlanNode.subqueryFilter;

public class TestCanonicalize {
  @Test
  void test() {
    final Plan plan =
        canonicalize(wrap(subqueryFilter(plainFilter(plainFilter(null)), null)).setup());
    assertEquals("PlainFilter(PlainFilter(SubqueryFilter(Input,Input)))", plan.toString());
  }
}
