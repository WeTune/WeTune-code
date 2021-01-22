package sjtu.ipads.wtune.superopt;

import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.superopt.core.Graph;
import sjtu.ipads.wtune.superopt.core.Substitution;
import sjtu.ipads.wtune.superopt.internal.Placeholder;
import sjtu.ipads.wtune.symsolver.core.Constraint;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static sjtu.ipads.wtune.superopt.internal.Placeholder.numbering;
import static sjtu.ipads.wtune.superopt.operator.Operator.innerJoin;
import static sjtu.ipads.wtune.superopt.operator.Operator.proj;
import static sjtu.ipads.wtune.symsolver.core.Constraint.*;

public class TestRebuild {
  @Test
  void test() {
    final Graph g0 = Graph.wrap(proj(innerJoin(null, null))).setup();
    final Graph g1 = Graph.wrap(proj(null)).setup();
    final Map<String, Placeholder> sym = numbering().number(g0, g1).placeholders();
    final List<Constraint> constraints =
        List.of(
            tableEq(sym.get("t0"), sym.get("t2")),
            pickEq(sym.get("c0"), sym.get("c2")),
            pickEq(sym.get("c1"), sym.get("c3")),
            pickFrom(sym.get("c0"), sym.get("t1")),
            pickFrom(sym.get("c1"), sym.get("t0")),
            pickFrom(sym.get("c2"), sym.get("t1")),
            pickFrom(sym.get("c3"), sym.get("t2")),
            reference(sym.get("t0"), sym.get("c1"), sym.get("t1"), sym.get("c2")));

    final Substitution sub0 = Substitution.build(g0, g1, constraints);
    final String str0 = sub0.toString();
    assertEquals(
        "Proj<c0>(InnerJoin<c1 c2>(Input<t0>,Input<t1>))|Proj<c3>(Input<t2>)|TableEq(t0,t2);PickEq(c0,c2);PickEq(c1,c3);PickFrom(c0,[t1]);"
            + "PickFrom(c1,[t0]);PickFrom(c2,[t1]);PickFrom(c3,[t2]);Reference(t0,c1,t1,c2)",
        str0);
    final Substitution sub1 = Substitution.rebuild(str0);
    assertEquals(
        "Proj<c0>(InnerJoin<c1 c2>(Input<t0>,Input<t1>))", sub1.g0().toInformativeString());
    assertEquals("Proj<c3>(Input<t2>)", sub1.g1().toInformativeString());
    assertEquals(sub1.toString(), str0);
  }
}
