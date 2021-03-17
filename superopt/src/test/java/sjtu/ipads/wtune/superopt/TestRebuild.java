package sjtu.ipads.wtune.superopt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static sjtu.ipads.wtune.superopt.fragment.Operator.innerJoin;
import static sjtu.ipads.wtune.superopt.fragment.Operator.proj;
import static sjtu.ipads.wtune.symsolver.core.Constraint.pickEq;
import static sjtu.ipads.wtune.symsolver.core.Constraint.pickFrom;
import static sjtu.ipads.wtune.symsolver.core.Constraint.reference;
import static sjtu.ipads.wtune.symsolver.core.Constraint.tableEq;

import com.google.common.collect.Lists;
import java.util.List;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Numbering;
import sjtu.ipads.wtune.superopt.optimizer.Substitution;
import sjtu.ipads.wtune.symsolver.core.Constraint;

public class TestRebuild {
  @Test
  void test() {
    final Fragment g0 = Fragment.wrap(proj(innerJoin(null, null))).setup();
    final Fragment g1 = Fragment.wrap(proj(null)).setup();
    final Numbering numbering = Numbering.make();
    numbering.number(g0, g1);
    final List<Constraint> constraints =
        Lists.newArrayList(
            tableEq(numbering.placeholderOf("t0"), numbering.placeholderOf("t2")),
            pickEq(numbering.placeholderOf("c0"), numbering.placeholderOf("c2")),
            pickEq(numbering.placeholderOf("c1"), numbering.placeholderOf("c3")),
            pickFrom(numbering.placeholderOf("c0"), numbering.placeholderOf("t1")),
            pickFrom(numbering.placeholderOf("c1"), numbering.placeholderOf("t0")),
            pickFrom(numbering.placeholderOf("c2"), numbering.placeholderOf("t1")),
            pickFrom(numbering.placeholderOf("c3"), numbering.placeholderOf("t2")),
            reference(
                numbering.placeholderOf("t0"),
                numbering.placeholderOf("c1"),
                numbering.placeholderOf("t1"),
                numbering.placeholderOf("c2")));

    final Substitution sub0 = Substitution.make(g0, g1, numbering, constraints);
    final String str0 = sub0.toString();
    assertEquals(
        "Proj<c0>(InnerJoin<c1 c2>(Input<t0>,Input<t1>))|Proj<c3>(Input<t2>)|TableEq(t0,t2);PickEq(c0,c2);PickEq(c1,c3);"
            + "Reference(t0,c1,t1,c2);PickFrom(c0,[t1]);PickFrom(c1,[t0]);PickFrom(c2,[t1]);PickFrom(c3,[t2])",
        str0);
    final Substitution sub1 = Substitution.rebuild(str0);
    assertEquals(sub1.toString(), str0);
  }
}
