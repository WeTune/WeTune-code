package sjtu.ipads.wtune.superopt;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.superopt.fragment1.Op;
import sjtu.ipads.wtune.superopt.substitution.Substitution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static sjtu.ipads.wtune.superopt.substitution.SubstitutionSupport.cutSubstitution;

@Tag("optimizer")
@Tag("fast")
public class TestCutSubstitution {
  @Test
  void test() {
    final String str =
        "Filter<p0 a0>(InnerJoin<a1 a2>(Input<t0>,Proj<a3>(Filter<p1 a4>(Input<t1>))))|Filter<p2 a5>(InnerJoin<a6 a7>(Input<t2>,Proj*<a8>(Input<t3>)))|AttrsEq(a0,a1);AttrsEq(a0,a5);AttrsEq(a0,a6);AttrsEq(a0,a7);AttrsEq(a0,a8);AttrsEq(a1,a5);AttrsEq(a1,a6);AttrsEq(a1,a7);AttrsEq(a1,a8);AttrsEq(a2,a3);AttrsEq(a2,a4);AttrsEq(a3,a4);AttrsEq(a5,a6);AttrsEq(a5,a7);AttrsEq(a5,a8);AttrsEq(a6,a7);AttrsEq(a6,a8);AttrsEq(a7,a8);AttrsSub(a0,t0);AttrsSub(a1,t0);AttrsSub(a2,a3);AttrsSub(a3,t1);AttrsSub(a4,t1);AttrsSub(a5,t2);AttrsSub(a6,t2);AttrsSub(a7,a8);AttrsSub(a8,t3);NotNull(t0,a0);NotNull(t0,a1);NotNull(t1,a3);NotNull(t1,a4);NotNull(t2,a5);NotNull(t2,a6);NotNull(t3,a8);PredicateEq(p0,p1);PredicateEq(p0,p2);PredicateEq(p1,p2);Reference(t0,a1,t1,a3);TableEq(t0,t1);TableEq(t0,t2);TableEq(t0,t3);TableEq(t1,t2);TableEq(t1,t3);TableEq(t2,t3);Unique(t1,a3);Unique(t1,a4)";
    final Substitution sub = Substitution.parse(str);
    final Op lhsCutPoint = sub._0().root().predecessors()[0].predecessors()[1];
    final Op rhsCutPoint = sub._1().root().predecessors()[0].predecessors()[1];
    final var pair = cutSubstitution(sub, lhsCutPoint, rhsCutPoint);
    assertEquals(
        "Filter<p0 a0>(InnerJoin<a1 a2>(Input<t0>,Input<t1>))|Filter<p1 a3>(InnerJoin<a4 a5>(Input<t2>,Input<t3>))|AttrsEq(a0,a1);AttrsEq(a0,a3);AttrsEq(a0,a4);AttrsEq(a0,a5);AttrsEq(a1,a3);AttrsEq(a1,a4);AttrsEq(a1,a5);AttrsEq(a3,a4);AttrsEq(a3,a5);AttrsEq(a4,a5);AttrsSub(a0,t0);AttrsSub(a1,t0);AttrsSub(a3,t2);AttrsSub(a4,t2);NotNull(t0,a0);NotNull(t0,a1);NotNull(t2,a3);NotNull(t2,a4);PredicateEq(p0,p1);TableEq(t0,t2);TableEq(t1,t3)",
        pair.getLeft().canonicalStringify());
    assertEquals(
        "Proj<a0>(Filter<p0 a1>(Input<t0>))|Proj*<a2>(Input<t1>)|AttrsEq(a0,a1);AttrsSub(a0,t0);AttrsSub(a1,t0);AttrsSub(a2,t1);NotNull(t0,a0);NotNull(t0,a1);NotNull(t1,a2);TableEq(t0,t1);Unique(t0,a0);Unique(t0,a1)",
        pair.getRight().canonicalStringify());
  }
}
