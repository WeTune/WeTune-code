package sjtu.ipads.wtune.superopt;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.superopt.fragment1.Fragment;
import sjtu.ipads.wtune.superopt.substitution.Substitution;

import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.prover.ProverSupport.mkLogicCtx;
import static sjtu.ipads.wtune.sqlparser.plan1.PlanSupport.disambiguate;
import static sjtu.ipads.wtune.sqlparser.plan1.PlanSupport.translateAsAst;
import static sjtu.ipads.wtune.superopt.constraint.ConstraintSupport.enumConstraints;
import static sjtu.ipads.wtune.superopt.fragment1.FragmentSupport.translateAsPlan;

@Tag("slow")
@Tag("enumeration")
public class TestEnumeration {
  private static boolean echo = true;

  private static void printReadable(Substitution substitution) {
    final var pair = translateAsPlan(substitution, false);
    System.out.println("sub: " + substitution);
    System.out.println(" q0: " + translateAsAst(disambiguate(pair.getLeft())));
    System.out.println(" q1: " + translateAsAst(disambiguate(pair.getRight())));
  }

  private static void doTest(String fragment0, String fragment1, String... expectation) {
    final Fragment f0 = Fragment.parse(fragment0, null);
    final Fragment f1 = Fragment.parse(fragment1, null);
    final List<Substitution> results = enumConstraints(f0, f1, mkLogicCtx());

    if (echo) results.forEach(TestEnumeration::printReadable);

    assertTrue(listMap(results, Object::toString).containsAll(asList(expectation)));
  }

  @Test
  void testInnerJoinElimination0() {
    doTest(
        "Proj(InnerInnerJoin(Input,Input))",
        "Proj(Input)",
        "Proj<a0>(InnerInnerJoin<a2 a3>(Input<t0>,Input<t1>))|Proj<a4>(Input<t2>)|TableEq(t0,t2);AttrsEq(a0,a4);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a0,t0);AttrsSub(a4,t2);Unique(t1,a3);NotNull(t0,a2);Reference(t0,a2,t1,a3)");
  }

  @Test
  void testInnerJoinElimination1() {
    doTest(
        "Proj*(InnerInnerJoin(Input,Input))",
        "Proj*(Input)",
        "Proj*<a0>(InnerInnerJoin<a2 a3>(Input<t0>,Input<t1>))|Proj*<a4>(Input<t2>)|TableEq(t0,t2);AttrsEq(a0,a4);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a0,t0);AttrsSub(a4,t2);NotNull(t0,a2);Reference(t0,a2,t1,a3)",
        "Proj*<a0>(InnerInnerJoin<a2 a3>(Input<t0>,Input<t1>))|Proj*<a4>(Input<t2>)|TableEq(t0,t2);AttrsEq(a0,a3);AttrsEq(a2,a4);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a0,t1);AttrsSub(a4,t2);NotNull(t0,a2);NotNull(t2,a4);Reference(t0,a2,t1,a3)");
  }

  @Test
  void testInnerJoinElimination2() {
    doTest(
        "Proj(PlainFilter(InnerJoin(Input,Input)))",
        "Proj(PlainFilter(Input))",
        "Proj<a0>(Filter<p0 a2>(InnerJoin<a3 a4>(Input<t0>,Input<t1>)))|Proj<a5>(Filter<p1 a7>(Input<t2>))|TableEq(t0,t2);AttrsEq(a0,a5);AttrsEq(a2,a7);PredicateEq(p0,p1);AttrsSub(a3,t0);AttrsSub(a4,t1);AttrsSub(a2,t0);AttrsSub(a0,t0);AttrsSub(a7,t2);AttrsSub(a5,t2);Unique(t1,a4);NotNull(t0,a3);Reference(t0,a3,t1,a4)",
        "Proj<a0>(Filter<p0 a2>(InnerJoin<a3 a4>(Input<t0>,Input<t1>)))|Proj<a5>(Filter<p1 a7>(Input<t2>))|TableEq(t0,t2);AttrsEq(a0,a5);AttrsEq(a2,a4);AttrsEq(a3,a7);PredicateEq(p0,p1);AttrsSub(a3,t0);AttrsSub(a4,t1);AttrsSub(a2,t1);AttrsSub(a0,t0);AttrsSub(a7,t2);AttrsSub(a5,t2);Unique(t1,a4);Unique(t1,a2);NotNull(t0,a3);NotNull(t2,a7);Reference(t0,a3,t1,a4)");
  }

  @Test
  void testInnerJoinElimination3() {
    doTest(
        "Proj*(PlainFilter(InnerJoin(Input,Input)))",
        "Proj*(PlainFilter(Input))",
        "Proj*<a0>(Filter<p0 a2>(InnerJoin<a3 a4>(Input<t0>,Input<t1>)))|Proj*<a5>(Filter<p1 a7>(Input<t2>))|TableEq(t0,t2);AttrsEq(a0,a5);AttrsEq(a2,a7);PredicateEq(p0,p1);AttrsSub(a3,t0);AttrsSub(a4,t1);AttrsSub(a2,t0);AttrsSub(a0,t0);AttrsSub(a7,t2);AttrsSub(a5,t2);NotNull(t0,a3);Reference(t0,a3,t1,a4)");
  }

  @Test
  void testLeftJoinElimination0() {
    doTest(
        "Proj(LeftJoin(Input,Input))",
        "Proj(Input)",
        "Proj<a0>(LeftJoin<a2 a3>(Input<t0>,Input<t1>))|Proj<a4>(Input<t2>)|TableEq(t0,t2);AttrsEq(a0,a4);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a0,t0);AttrsSub(a4,t2);Unique(t1,a3);NotNull(t0,a2)");
  }

  @Test
  void testLeftJoinElimination1() {
    doTest(
        "Proj*(LeftJoin(Input,Input))",
        "Proj*(Input)",
        "Proj*<a0>(LeftJoin<a2 a3>(Input<t0>,Input<t1>))|Proj*<a4>(Input<t2>)|TableEq(t0,t2);AttrsEq(a0,a4);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a0,t0);AttrsSub(a4,t2)",
        "Proj*<a0>(LeftJoin<a2 a3>(Input<t0>,Input<t1>))|Proj*<a4>(Input<t2>)|TableEq(t0,t2);AttrsEq(a0,a3);AttrsEq(a2,a4);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a0,t1);AttrsSub(a4,t2);Reference(t0,a2,t1,a3)");
  }

  @Test
  void testLeftJoinElimination2() {
    doTest(
        "Proj(PlainFilter(LeftJoin(Input,Input)))",
        "Proj(PlainFilter(Input))",
        "Proj<a0>(Filter<p0 a2>(LeftJoin<a3 a4>(Input<t0>,Input<t1>)))|Proj<a5>(Filter<p1 a7>(Input<t2>))|TableEq(t0,t2);AttrsEq(a0,a5);AttrsEq(a2,a7);PredicateEq(p0,p1);AttrsSub(a3,t0);AttrsSub(a4,t1);AttrsSub(a2,t0);AttrsSub(a0,t0);AttrsSub(a7,t2);AttrsSub(a5,t2);Unique(t1,a4);NotNull(t0,a3)");
  }

  @Test
  void testLeftJoinElimination3() {
    doTest(
        "Proj*(PlainFilter(LeftJoin(Input,Input)))",
        "Proj*(PlainFilter(Input))",
        "Proj*<a0>(Filter<p0 a2>(LeftJoin<a3 a4>(Input<t0>,Input<t1>)))|Proj*<a5>(Filter<p1 a7>(Input<t2>))|TableEq(t0,t2);AttrsEq(a0,a5);AttrsEq(a2,a7);PredicateEq(p0,p1);AttrsSub(a3,t0);AttrsSub(a4,t1);AttrsSub(a2,t0);AttrsSub(a0,t0);AttrsSub(a7,t2);AttrsSub(a5,t2)",
        "Proj*<a0>(Filter<p0 a2>(LeftJoin<a3 a4>(Input<t0>,Input<t1>)))|Proj*<a5>(Filter<p1 a7>(Input<t2>))|TableEq(t0,t2);AttrsEq(a0,a5);AttrsEq(a2,a4);AttrsEq(a3,a7);PredicateEq(p0,p1);AttrsSub(a3,t0);AttrsSub(a4,t1);AttrsSub(a2,t1);AttrsSub(a0,t0);AttrsSub(a7,t2);AttrsSub(a5,t2);Reference(t0,a3,t1,a4)");
  }

  @Test
  void testLeftJoin2InnerJoin() {
    doTest(
        "LeftJoin(Input,Input)",
        "InnerJoin(Input,Input)",
        "LeftJoin<a0 a1>(Input<t0>,Input<t1>)|InnerJoin<a2 a3>(Input<t2>,Input<t3>)|TableEq(t0,t2);TableEq(t1,t3);AttrsEq(a0,a2);AttrsEq(a1,a3);AttrsSub(a0,t0);AttrsSub(a1,t1);AttrsSub(a2,t2);AttrsSub(a3,t3);Unique(t1,a1);Unique(t3,a3);NotNull(t0,a0);NotNull(t2,a2);Reference(t0,a0,t1,a1);Reference(t2,a2,t3,a3)");
  }

  @Test
  void testIN2InnerJoin0() {
    doTest(
        "Proj(InSubFilter(Input,Proj(Input)))",
        "Proj(InnerJoin(Input,Input))",
        "Proj<a0>(SubFilter<a2>(Input<t0>,Proj<a3>(Input<t1>)))|Proj<a5>(InnerJoin<a7 a8>(Input<t2>,Input<t3>))|TableEq(t0,t2);TableEq(t1,t3);AttrsEq(a0,a5);AttrsEq(a2,a7);AttrsEq(a3,a8);AttrsSub(a3,t1);AttrsSub(a2,t0);AttrsSub(a0,t0);AttrsSub(a7,t2);AttrsSub(a8,t3);AttrsSub(a5,t2);Unique(t1,a3);Unique(t3,a8)");
  }

  @Test
  void testIN2InnerJoin1() {
    doTest(
        "Proj*(InSubFilter(Input,Proj(Input)))",
        "Proj*(InnerJoin(Input,Input))",
        "Proj*<a0>(SubFilter<a2>(Input<t0>,Proj<a3>(Input<t1>)))|Proj*<a5>(InnerJoin<a7 a8>(Input<t2>,Input<t3>))|TableEq(t0,t2);TableEq(t1,t3);AttrsEq(a0,a5);AttrsEq(a2,a7);AttrsEq(a3,a8);AttrsSub(a3,t1);AttrsSub(a2,t0);AttrsSub(a0,t0);AttrsSub(a7,t2);AttrsSub(a8,t3);AttrsSub(a5,t2)");
  }

  @Test
  void testPlainFilterCollapsing() {
    doTest(
        "PlainFilter(PlainFilter(Input))",
        "PlainFilter(Input)",
        "Filter<p0 a0>(Filter<p1 a1>(Input<t0>))|Filter<p2 a2>(Input<t1>)|TableEq(t0,t1);AttrsEq(a0,a1);AttrsEq(a0,a2);AttrsEq(a1,a2);PredicateEq(p0,p1);PredicateEq(p0,p2);PredicateEq(p1,p2);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a2,t1)");
  }

  @Test
  void testINSubFilterCollapsing() {
    doTest(
        "InSubFilter(InSubFilter(Input,Input),Input)",
        "InSubFilter(Input,Input)",
        "SubFilter<a0>(SubFilter<a1>(Input<t0>,Input<t1>),Input<t2>)|SubFilter<a2>(Input<t3>,Input<t4>)|TableEq(t0,t3);TableEq(t1,t2);TableEq(t1,t4);TableEq(t2,t4);AttrsEq(a0,a1);AttrsEq(a0,a2);AttrsEq(a1,a2);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a2,t3)");
  }

  @Test
  void testProjCollapsing0() {
    doTest(
        "Proj(Proj(Input))",
        "Proj(Input)",
        "Proj<a0>(Proj<a2>(Input<t0>))|Proj<a4>(Input<t1>)|TableEq(t0,t1);AttrsEq(a2,a4);AttrsSub(a2,t0);AttrsSub(a0,a3);AttrsSub(a4,t1)");
  }
}
