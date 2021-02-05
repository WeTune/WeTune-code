package sjtu.ipads.wtune.superopt;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.superopt.plan.Plan;
import sjtu.ipads.wtune.superopt.optimization.Substitution;
import sjtu.ipads.wtune.superopt.util.PlaceholderNumbering;
import sjtu.ipads.wtune.symsolver.core.Constraint;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static sjtu.ipads.wtune.superopt.plan.PlanNode.innerJoin;
import static sjtu.ipads.wtune.superopt.plan.PlanNode.proj;
import static sjtu.ipads.wtune.symsolver.core.Constraint.*;

public class TestRebuild {
  @Test
  void test() {
    final Plan g0 = Plan.wrap(proj(innerJoin(null, null))).setup();
    final Plan g1 = Plan.wrap(proj(null)).setup();
    final PlaceholderNumbering numbering = PlaceholderNumbering.build();
    numbering.number(g0, g1);
    final List<Constraint> constraints =
        Lists.newArrayList(
            tableEq(numbering.find("t0"), numbering.find("t2")),
            pickEq(numbering.find("c0"), numbering.find("c2")),
            pickEq(numbering.find("c1"), numbering.find("c3")),
            pickFrom(numbering.find("c0"), numbering.find("t1")),
            pickFrom(numbering.find("c1"), numbering.find("t0")),
            pickFrom(numbering.find("c2"), numbering.find("t1")),
            pickFrom(numbering.find("c3"), numbering.find("t2")),
            reference(
                numbering.find("t0"),
                numbering.find("c1"),
                numbering.find("t1"),
                numbering.find("c2")));

    final Substitution sub0 = Substitution.build(g0, g1, numbering, constraints);
    final String str0 = sub0.toString();
    assertEquals(
        "Proj<c0>(InnerJoin<c1 c2>(Input<t0>,Input<t1>))|Proj<c3>(Input<t2>)|TableEq(t0,t2);PickEq(c0,c2);PickEq(c1,c3);PickFrom(c0,[t1]);"
            + "PickFrom(c1,[t0]);PickFrom(c2,[t1]);PickFrom(c3,[t2]);Reference(t0,c1,t1,c2)",
        str0);
    final Substitution sub1 = Substitution.rebuild(str0);
    assertEquals(sub1.toString(), str0);
  }
}
