package sjtu.ipads.wtune.superopt;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.superopt.constraint.ConstraintEnumerator;
import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.substitution.Substitution;

import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.prover.ProverSupport.mkLogicCtx;
import static sjtu.ipads.wtune.sqlparser.plan.PlanSupport.disambiguate;
import static sjtu.ipads.wtune.sqlparser.plan.PlanSupport.translateAsAst;
import static sjtu.ipads.wtune.superopt.constraint.ConstraintSupport.enumConstraints;
import static sjtu.ipads.wtune.superopt.constraint.ConstraintSupport.mkConstraintEnumerator;
import static sjtu.ipads.wtune.superopt.substitution.SubstitutionSupport.translateAsPlan;

@Tag("slow")
@Tag("enumeration")
public class TestEnumeration {
  private static boolean echo = true;

  private static void printReadable(Substitution substitution) {
    final var pair = translateAsPlan(substitution, false, true);
    System.out.println("sub: " + substitution);
    System.out.println(" q0: " + translateAsAst(disambiguate(pair.getLeft())));
    System.out.println(" q1: " + translateAsAst(disambiguate(pair.getRight())));
  }

  private static void doTest(String fragment0, String fragment1, String... expectation) {
    final Fragment f0 = Fragment.parse(fragment0, null);
    final Fragment f1 = Fragment.parse(fragment1, null);
    long startTime = System.currentTimeMillis();
    final List<Substitution> results = enumConstraints(f0, f1, mkLogicCtx());
    long endTime = System.currentTimeMillis();
    System.out.println("Execution time: " + (endTime - startTime) + "ms.");

    //    if (echo) results.forEach(TestEnumeration::printReadable);
    results.forEach(System.out::println);

    assertTrue(listMap(results, Object::toString).containsAll(asList(expectation)));
  }

  @Test
  void testInnerJoinElimination0() {
    doTest(
        "Proj(InnerJoin(Input,Input))",
        "Proj(Input)"
//            "Proj<a0>(InnerJoin<a1 a2>(Input<t0>,Input<t1>))|Proj<a3>(Input<t2>)|TableEq(t0,t1);TableEq(t0,t2);TableEq(t1,t2);AttrsEq(a0,a3);AttrsEq(a1,a2);AttrsSub(a1,t0);AttrsSub(a2,t1);AttrsSub(a0,t0);AttrsSub(a3,t2);Unique(t0,a1);Unique(t1,a2);NotNull(t0,a1);NotNull(t1,a2)",
//            "Proj<a0>(InnerJoin<a1 a2>(Input<t0>,Input<t1>))|Proj<a3>(Input<t2>)|TableEq(t0,t2);AttrsEq(a0,a3);AttrsSub(a1,t0);AttrsSub(a2,t1);AttrsSub(a0,t0);AttrsSub(a3,t2);Unique(t1,a2);NotNull(t0,a1);Reference(t0,a1,t1,a2)"
    );
  }

  @Test
  void testInnerJoinElimination1() {
    doTest(
        "Proj*(InnerJoin(Input,Input))",
        "Proj*(Input)");
//            "Proj*<a0>(InnerJoin<a1 a2>(Input<t0>,Input<t1>))|Proj*<a3>(Input<t2>)|TableEq(t0,t1);TableEq(t0,t2);TableEq(t1,t2);AttrsEq(a0,a3);AttrsEq(a1,a2);AttrsSub(a1,t0);AttrsSub(a2,t1);AttrsSub(a0,t0);AttrsSub(a3,t2);NotNull(t0,a1);NotNull(t1,a2)",
//            "Proj*<a0>(InnerJoin<a1 a2>(Input<t0>,Input<t1>))|Proj*<a3>(Input<t2>)|TableEq(t0,t1);TableEq(t0,t2);TableEq(t1,t2);AttrsEq(a0,a3);AttrsEq(a1,a2);AttrsSub(a1,t0);AttrsSub(a2,t1);AttrsSub(a0,t1);AttrsSub(a3,t2);NotNull(t0,a1);NotNull(t1,a2)",
//            "Proj*<a0>(InnerJoin<a1 a2>(Input<t0>,Input<t1>))|Proj*<a3>(Input<t2>)|TableEq(t0,t2);AttrsEq(a0,a2);AttrsEq(a1,a3);AttrsSub(a1,t0);AttrsSub(a2,t1);AttrsSub(a0,t1);AttrsSub(a3,t2);NotNull(t0,a1);NotNull(t2,a3);Reference(t0,a1,t1,a2)",
//            "Proj*<a0>(InnerJoin<a1 a2>(Input<t0>,Input<t1>))|Proj*<a3>(Input<t2>)|TableEq(t0,t2);AttrsEq(a0,a3);AttrsSub(a1,t0);AttrsSub(a2,t1);AttrsSub(a0,t0);AttrsSub(a3,t2);NotNull(t0,a1);Reference(t0,a1,t1,a2)"
  }

  @Test
  void testInnerJoinElimination2() {
    doTest(
        "Proj(PlainFilter(InnerJoin(Input,Input)))",
        "Proj(PlainFilter(Input))");
//            "Proj<a0>(Filter<p0 a1>(InnerJoin<a2 a3>(Input<t0>,Input<t1>)))|Proj<a4>(Filter<p1 a5>(Input<t2>))|TableEq(t0,t1);TableEq(t0,t2);TableEq(t1,t2);AttrsEq(a0,a4);AttrsEq(a1,a5);AttrsEq(a2,a3);PredicateEq(p0,p1);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a5,t2);AttrsSub(a4,t2);Unique(t0,a2);Unique(t1,a3);NotNull(t0,a2);NotNull(t1,a3)",
//            "Proj<a0>(Filter<p0 a1>(InnerJoin<a2 a3>(Input<t0>,Input<t1>)))|Proj<a4>(Filter<p1 a5>(Input<t2>))|TableEq(t0,t2);AttrsEq(a0,a4);AttrsEq(a1,a3);AttrsEq(a2,a5);PredicateEq(p0,p1);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a1,t1);AttrsSub(a0,t0);AttrsSub(a5,t2);AttrsSub(a4,t2);Unique(t1,a3);Unique(t1,a1);NotNull(t0,a2);NotNull(t2,a5);Reference(t0,a2,t1,a3)",
//            "Proj<a0>(Filter<p0 a1>(InnerJoin<a2 a3>(Input<t0>,Input<t1>)))|Proj<a4>(Filter<p1 a5>(Input<t2>))|TableEq(t0,t2);AttrsEq(a0,a4);AttrsEq(a1,a5);PredicateEq(p0,p1);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a5,t2);AttrsSub(a4,t2);Unique(t1,a3);NotNull(t0,a2);Reference(t0,a2,t1,a3)",
  }

  @Test
  void testInnerJoinElimination3() {
    doTest(
        "Proj*(PlainFilter(InnerJoin(Input,Input)))",
        "Proj*(PlainFilter(Input))");
//            "Proj*<a0>(Filter<p0 a1>(InnerJoin<a2 a3>(Input<t0>,Input<t1>)))|Proj*<a4>(Filter<p1 a5>(Input<t2>))|TableEq(t0,t1);TableEq(t0,t2);TableEq(t1,t2);AttrsEq(a0,a4);AttrsEq(a1,a5);AttrsEq(a2,a3);PredicateEq(p0,p1);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a5,t2);AttrsSub(a4,t2);NotNull(t0,a2);NotNull(t1,a3)",
//            "Proj*<a0>(Filter<p0 a1>(InnerJoin<a2 a3>(Input<t0>,Input<t1>)))|Proj*<a4>(Filter<p1 a5>(Input<t2>))|TableEq(t0,t2);AttrsEq(a0,a1);AttrsEq(a0,a3);AttrsEq(a1,a3);AttrsEq(a2,a4);AttrsEq(a2,a5);AttrsEq(a4,a5);PredicateEq(p0,p1);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a1,t1);AttrsSub(a0,t1);AttrsSub(a5,t2);AttrsSub(a4,t2);NotNull(t0,a2);NotNull(t2,a5);NotNull(t2,a4);Reference(t0,a2,t1,a3)",
//            "Proj*<a0>(Filter<p0 a1>(InnerJoin<a2 a3>(Input<t0>,Input<t1>)))|Proj*<a4>(Filter<p1 a5>(Input<t2>))|TableEq(t0,t2);AttrsEq(a0,a3);AttrsEq(a1,a5);AttrsEq(a2,a4);PredicateEq(p0,p1);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a1,t0);AttrsSub(a0,t1);AttrsSub(a5,t2);AttrsSub(a4,t2);NotNull(t0,a2);NotNull(t2,a4);Reference(t0,a2,t1,a3)",
//            "Proj*<a0>(Filter<p0 a1>(InnerJoin<a2 a3>(Input<t0>,Input<t1>)))|Proj*<a4>(Filter<p1 a5>(Input<t2>))|TableEq(t0,t2);AttrsEq(a0,a4);AttrsEq(a1,a3);AttrsEq(a2,a5);PredicateEq(p0,p1);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a1,t1);AttrsSub(a0,t0);AttrsSub(a5,t2);AttrsSub(a4,t2);NotNull(t0,a2);NotNull(t2,a5);Reference(t0,a2,t1,a3)",
//            "Proj*<a0>(Filter<p0 a1>(InnerJoin<a2 a3>(Input<t0>,Input<t1>)))|Proj*<a4>(Filter<p1 a5>(Input<t2>))|TableEq(t0,t2);AttrsEq(a0,a4);AttrsEq(a1,a5);PredicateEq(p0,p1);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a5,t2);AttrsSub(a4,t2);NotNull(t0,a2);Reference(t0,a2,t1,a3)",
  }

  @Test
  void testLeftJoinElimination0() {
    doTest(
        "Proj(LeftJoin(Input,Input))",
        "Proj(Input)");
//            "Proj<a0>(LeftJoin<a1 a2>(Input<t0>,Input<t1>))|Proj<a3>(Input<t2>)|TableEq(t0,t1);TableEq(t0,t2);TableEq(t1,t2);AttrsEq(a0,a3);AttrsSub(a1,t0);AttrsSub(a2,t1);AttrsSub(a0,t0);AttrsSub(a3,t2);Unique(t1,a2)",
//            "Proj<a0>(LeftJoin<a1 a2>(Input<t0>,Input<t1>))|Proj<a3>(Input<t2>)|TableEq(t0,t2);AttrsEq(a0,a1);AttrsEq(a0,a3);AttrsEq(a1,a3);AttrsSub(a1,t0);AttrsSub(a2,t1);AttrsSub(a0,t0);AttrsSub(a3,t2);Unique(t1,a2)",
//            "Proj<a0>(LeftJoin<a1 a2>(Input<t0>,Input<t1>))|Proj<a3>(Input<t2>)|TableEq(t0,t2);AttrsEq(a0,a3);AttrsSub(a1,t0);AttrsSub(a2,t1);AttrsSub(a0,t0);AttrsSub(a3,t2);Unique(t1,a2);NotNull(t0,a1)",
  }

  @Test
  void testLeftJoinElimination1() {
    doTest(
        "Proj*(LeftJoin(Input,Input))",
        "Proj*(Input)");
//            "Proj*<a0>(LeftJoin<a1 a2>(Input<t0>,Input<t1>))|Proj*<a3>(Input<t2>)|TableEq(t0,t1);TableEq(t0,t2);TableEq(t1,t2);AttrsEq(a0,a1);AttrsEq(a0,a2);AttrsEq(a0,a3);AttrsEq(a1,a2);AttrsEq(a1,a3);AttrsEq(a2,a3);AttrsSub(a1,t0);AttrsSub(a2,t1);AttrsSub(a0,t1);AttrsSub(a3,t2)",
//            "Proj*<a0>(LeftJoin<a1 a2>(Input<t0>,Input<t1>))|Proj*<a3>(Input<t2>)|TableEq(t0,t1);TableEq(t0,t2);TableEq(t1,t2);AttrsEq(a0,a3);AttrsEq(a1,a2);AttrsSub(a1,t0);AttrsSub(a2,t1);AttrsSub(a0,t1);AttrsSub(a3,t2);NotNull(t0,a1);NotNull(t1,a2)",
//            "Proj*<a0>(LeftJoin<a1 a2>(Input<t0>,Input<t1>))|Proj*<a3>(Input<t2>)|TableEq(t0,t2);AttrsEq(a0,a2);AttrsEq(a1,a3);AttrsSub(a1,t0);AttrsSub(a2,t1);AttrsSub(a0,t1);AttrsSub(a3,t2);Reference(t0,a1,t1,a2)",
//            "Proj*<a0>(LeftJoin<a1 a2>(Input<t0>,Input<t1>))|Proj*<a3>(Input<t2>)|TableEq(t0,t2);AttrsEq(a0,a3);AttrsSub(a1,t0);AttrsSub(a2,t1);AttrsSub(a0,t0);AttrsSub(a3,t2)",
  }

  @Test
  void testLeftJoinElimination2() {
    doTest(
        "Proj(Filter(LeftJoin(Input,Input)))",
        "Proj(Filter(Input))");
//            "Proj<a0>(Filter<p0 a1>(LeftJoin<a2 a3>(Input<t0>,Input<t1>)))|Proj<a4>(Filter<p1 a5>(Input<t2>))|TableEq(t0,t1);TableEq(t0,t2);TableEq(t1,t2);AttrsEq(a0,a4);AttrsEq(a1,a5);PredicateEq(p0,p1);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a5,t2);AttrsSub(a4,t2);Unique(t1,a3)",
//            "Proj<a0>(Filter<p0 a1>(LeftJoin<a2 a3>(Input<t0>,Input<t1>)))|Proj<a4>(Filter<p1 a5>(Input<t2>))|TableEq(t0,t2);AttrsEq(a0,a2);AttrsEq(a0,a4);AttrsEq(a0,a5);AttrsEq(a1,a3);AttrsEq(a2,a4);AttrsEq(a2,a5);AttrsEq(a4,a5);PredicateEq(p0,p1);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a1,t1);AttrsSub(a0,t0);AttrsSub(a5,t2);AttrsSub(a4,t2);Unique(t1,a3);Unique(t1,a1);Reference(t0,a2,t1,a3)",
//            "Proj<a0>(Filter<p0 a1>(LeftJoin<a2 a3>(Input<t0>,Input<t1>)))|Proj<a4>(Filter<p1 a5>(Input<t2>))|TableEq(t0,t2);AttrsEq(a0,a2);AttrsEq(a0,a4);AttrsEq(a1,a5);AttrsEq(a2,a4);PredicateEq(p0,p1);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a5,t2);AttrsSub(a4,t2);Unique(t1,a3)",
//            "Proj<a0>(Filter<p0 a1>(LeftJoin<a2 a3>(Input<t0>,Input<t1>)))|Proj<a4>(Filter<p1 a5>(Input<t2>))|TableEq(t0,t2);AttrsEq(a0,a4);AttrsEq(a1,a3);AttrsEq(a2,a5);PredicateEq(p0,p1);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a1,t1);AttrsSub(a0,t0);AttrsSub(a5,t2);AttrsSub(a4,t2);Unique(t1,a3);Unique(t1,a1);NotNull(t0,a2);NotNull(t2,a5);Reference(t0,a2,t1,a3)",
//            "Proj<a0>(Filter<p0 a1>(LeftJoin<a2 a3>(Input<t0>,Input<t1>)))|Proj<a4>(Filter<p1 a5>(Input<t2>))|TableEq(t0,t2);AttrsEq(a0,a4);AttrsEq(a1,a5);PredicateEq(p0,p1);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a5,t2);AttrsSub(a4,t2);Unique(t1,a3);NotNull(t0,a2)",
  }

  @Test
  void testLeftJoinElimination3() {
    doTest(
        "Proj*(Filter(LeftJoin(Input,Input)))",
        "Proj*(Filter(Input))");
//            "Proj*<a0>(Filter<p0 a1>(LeftJoin<a2 a3>(Input<t0>,Input<t1>)))|Proj*<a4>(Filter<p1 a5>(Input<t2>))|TableEq(t0,t1);TableEq(t0,t2);TableEq(t1,t2);AttrsEq(a0,a1);AttrsEq(a0,a2);AttrsEq(a0,a3);AttrsEq(a0,a4);AttrsEq(a0,a5);AttrsEq(a1,a2);AttrsEq(a1,a3);AttrsEq(a1,a4);AttrsEq(a1,a5);AttrsEq(a2,a3);AttrsEq(a2,a4);AttrsEq(a2,a5);AttrsEq(a3,a4);AttrsEq(a3,a5);AttrsEq(a4,a5);PredicateEq(p0,p1);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a1,t1);AttrsSub(a0,t1);AttrsSub(a5,t2);AttrsSub(a4,t2)",
//            "Proj*<a0>(Filter<p0 a1>(LeftJoin<a2 a3>(Input<t0>,Input<t1>)))|Proj*<a4>(Filter<p1 a5>(Input<t2>))|TableEq(t0,t1);TableEq(t0,t2);TableEq(t1,t2);AttrsEq(a0,a2);AttrsEq(a0,a3);AttrsEq(a0,a4);AttrsEq(a1,a5);AttrsEq(a2,a3);AttrsEq(a2,a4);AttrsEq(a3,a4);PredicateEq(p0,p1);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a1,t0);AttrsSub(a0,t1);AttrsSub(a5,t2);AttrsSub(a4,t2)",
//            "Proj*<a0>(Filter<p0 a1>(LeftJoin<a2 a3>(Input<t0>,Input<t1>)))|Proj*<a4>(Filter<p1 a5>(Input<t2>))|TableEq(t0,t1);TableEq(t0,t2);TableEq(t1,t2);AttrsEq(a0,a4);AttrsEq(a1,a2);AttrsEq(a1,a3);AttrsEq(a1,a5);AttrsEq(a2,a3);AttrsEq(a2,a5);AttrsEq(a3,a5);PredicateEq(p0,p1);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a1,t1);AttrsSub(a0,t0);AttrsSub(a5,t2);AttrsSub(a4,t2)",
//            "Proj*<a0>(Filter<p0 a1>(LeftJoin<a2 a3>(Input<t0>,Input<t1>)))|Proj*<a4>(Filter<p1 a5>(Input<t2>))|TableEq(t0,t2);AttrsEq(a0,a1);AttrsEq(a0,a3);AttrsEq(a1,a3);AttrsEq(a2,a4);AttrsEq(a2,a5);AttrsEq(a4,a5);PredicateEq(p0,p1);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a1,t1);AttrsSub(a0,t1);AttrsSub(a5,t2);AttrsSub(a4,t2);Reference(t0,a2,t1,a3)",
//            "Proj*<a0>(Filter<p0 a1>(LeftJoin<a2 a3>(Input<t0>,Input<t1>)))|Proj*<a4>(Filter<p1 a5>(Input<t2>))|TableEq(t0,t2);AttrsEq(a0,a3);AttrsEq(a1,a5);AttrsEq(a2,a4);PredicateEq(p0,p1);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a1,t0);AttrsSub(a0,t1);AttrsSub(a5,t2);AttrsSub(a4,t2);Reference(t0,a2,t1,a3)",
//            "Proj*<a0>(Filter<p0 a1>(LeftJoin<a2 a3>(Input<t0>,Input<t1>)))|Proj*<a4>(Filter<p1 a5>(Input<t2>))|TableEq(t0,t2);AttrsEq(a0,a4);AttrsEq(a1,a3);AttrsEq(a2,a5);PredicateEq(p0,p1);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a1,t1);AttrsSub(a0,t0);AttrsSub(a5,t2);AttrsSub(a4,t2);Reference(t0,a2,t1,a3)",
//            "Proj*<a0>(Filter<p0 a1>(LeftJoin<a2 a3>(Input<t0>,Input<t1>)))|Proj*<a4>(Filter<p1 a5>(Input<t2>))|TableEq(t0,t2);AttrsEq(a0,a4);AttrsEq(a1,a5);PredicateEq(p0,p1);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a5,t2);AttrsSub(a4,t2)",
  }

  @Test
  void testLeftJoin2InnerJoin() {
    doTest(
        "LeftJoin(Input,Input)",
        "InnerJoin(Input,Input)");
//            "LeftJoin<a0 a1>(Input<t0>,Input<t1>)|InnerJoin<a2 a3>(Input<t2>,Input<t3>)|TableEq(t0,t1);TableEq(t0,t2);TableEq(t0,t3);TableEq(t1,t2);TableEq(t1,t3);TableEq(t2,t3);AttrsEq(a0,a1);AttrsEq(a0,a2);AttrsEq(a0,a3);AttrsEq(a1,a2);AttrsEq(a1,a3);AttrsEq(a2,a3);AttrsSub(a0,t0);AttrsSub(a1,t1);AttrsSub(a2,t2);AttrsSub(a3,t3);NotNull(t0,a0);NotNull(t1,a1);NotNull(t2,a2);NotNull(t3,a3)",
//            "LeftJoin<a0 a1>(Input<t0>,Input<t1>)|InnerJoin<a2 a3>(Input<t2>,Input<t3>)|TableEq(t0,t2);TableEq(t1,t3);AttrsEq(a0,a2);AttrsEq(a1,a3);AttrsSub(a0,t0);AttrsSub(a1,t1);AttrsSub(a2,t2);AttrsSub(a3,t3);NotNull(t0,a0);NotNull(t2,a2);Reference(t0,a0,t1,a1);Reference(t2,a2,t3,a3)",
  }

  @Test
  void testIN2InnerJoin0() {
    doTest(
        "Proj(InSubFilter(Input,Proj(Input)))",
        "Proj(InnerJoin(Input,Input))");
//            "Proj<a0>(InSubFilter<a1>(Input<t0>,Proj<a2>(Input<t1>)))|Proj<a3>(InnerJoin<a4 a5>(Input<t2>,Input<t3>))|TableEq(t0,t1);TableEq(t0,t2);TableEq(t0,t3);TableEq(t1,t2);TableEq(t1,t3);TableEq(t2,t3);AttrsEq(a0,a1);AttrsEq(a0,a2);AttrsEq(a0,a3);AttrsEq(a1,a2);AttrsEq(a1,a3);AttrsEq(a2,a3);AttrsEq(a4,a5);AttrsSub(a2,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a4,t2);AttrsSub(a5,t3);AttrsSub(a3,t2);Unique(t2,a4);Unique(t3,a5);NotNull(t1,a2);NotNull(t0,a1);NotNull(t0,a0);NotNull(t2,a4);NotNull(t3,a5);NotNull(t2,a3)",
//            "Proj<a0>(InSubFilter<a1>(Input<t0>,Proj<a2>(Input<t1>)))|Proj<a3>(InnerJoin<a4 a5>(Input<t2>,Input<t3>))|TableEq(t0,t1);TableEq(t0,t2);TableEq(t0,t3);TableEq(t1,t2);TableEq(t1,t3);TableEq(t2,t3);AttrsEq(a0,a3);AttrsEq(a0,a4);AttrsEq(a0,a5);AttrsEq(a1,a2);AttrsEq(a3,a4);AttrsEq(a3,a5);AttrsEq(a4,a5);AttrsSub(a2,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a4,t2);AttrsSub(a5,t3);AttrsSub(a3,t2);Unique(t0,a0);Unique(t2,a4);Unique(t3,a5);Unique(t2,a3);NotNull(t1,a2);NotNull(t0,a1);NotNull(t0,a0);NotNull(t2,a4);NotNull(t3,a5);NotNull(t2,a3)",
//            "Proj<a0>(InSubFilter<a1>(Input<t0>,Proj<a2>(Input<t1>)))|Proj<a3>(InnerJoin<a4 a5>(Input<t2>,Input<t3>))|TableEq(t0,t1);TableEq(t0,t2);TableEq(t0,t3);TableEq(t1,t2);TableEq(t1,t3);TableEq(t2,t3);AttrsEq(a0,a3);AttrsEq(a0,a4);AttrsEq(a1,a2);AttrsEq(a1,a5);AttrsEq(a2,a5);AttrsEq(a3,a4);AttrsSub(a2,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a4,t2);AttrsSub(a5,t3);AttrsSub(a3,t2);Unique(t1,a2);Unique(t0,a1);Unique(t3,a5);NotNull(t1,a2);NotNull(t0,a1);NotNull(t0,a0);NotNull(t2,a4);NotNull(t3,a5);NotNull(t2,a3);Reference(t2,a4,t3,a5)",
//            "Proj<a0>(InSubFilter<a1>(Input<t0>,Proj<a2>(Input<t1>)))|Proj<a3>(InnerJoin<a4 a5>(Input<t2>,Input<t3>))|TableEq(t0,t1);TableEq(t0,t2);TableEq(t1,t2);AttrsEq(a0,a1);AttrsEq(a0,a2);AttrsEq(a0,a3);AttrsEq(a1,a2);AttrsEq(a1,a3);AttrsEq(a2,a3);AttrsSub(a2,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a4,t2);AttrsSub(a5,t3);AttrsSub(a3,t2);Unique(t3,a5);NotNull(t1,a2);NotNull(t0,a1);NotNull(t0,a0);NotNull(t2,a4);NotNull(t2,a3);Reference(t2,a4,t3,a5)",
//            "Proj<a0>(InSubFilter<a1>(Input<t0>,Proj<a2>(Input<t1>)))|Proj<a3>(InnerJoin<a4 a5>(Input<t2>,Input<t3>))|TableEq(t0,t1);TableEq(t0,t2);TableEq(t1,t2);AttrsEq(a0,a3);AttrsEq(a1,a2);AttrsEq(a1,a4);AttrsEq(a2,a4);AttrsSub(a2,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a4,t2);AttrsSub(a5,t3);AttrsSub(a3,t2);Unique(t3,a5);Reference(t2,a4,t3,a5)",
//            "Proj<a0>(InSubFilter<a1>(Input<t0>,Proj<a2>(Input<t1>)))|Proj<a3>(InnerJoin<a4 a5>(Input<t2>,Input<t3>))|TableEq(t0,t2);TableEq(t1,t3);AttrsEq(a0,a3);AttrsEq(a1,a4);AttrsEq(a2,a5);AttrsSub(a2,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a4,t2);AttrsSub(a5,t3);AttrsSub(a3,t2);Unique(t1,a2);Unique(t3,a5)",
//            "Proj<a0>(InSubFilter<a1>(Input<t0>,Proj<a2>(Input<t1>)))|Proj<a3>(InnerJoin<a4 a5>(Input<t2>,Input<t3>))|TableEq(t0,t3);TableEq(t1,t2);AttrsEq(a0,a1);AttrsEq(a0,a5);AttrsEq(a1,a5);AttrsEq(a2,a3);AttrsEq(a2,a4);AttrsEq(a3,a4);AttrsSub(a2,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a4,t2);AttrsSub(a5,t3);AttrsSub(a3,t2);Unique(t1,a2);Unique(t2,a4);Unique(t2,a3)",
  }

  @Test
  void testIN2InnerJoin1() {
    doTest(
        "Proj*(InSubFilter(Input,Proj(Input)))",
        "Proj*(InnerJoin(Input,Input))");
//            "Proj*<a0>(InSubFilter<a1>(Input<t0>,Proj<a2>(Input<t1>)))|Proj*<a3>(InnerJoin<a4 a5>(Input<t2>,Input<t3>))|TableEq(t0,t1);TableEq(t0,t2);TableEq(t0,t3);TableEq(t1,t2);TableEq(t1,t3);TableEq(t2,t3);AttrsEq(a0,a1);AttrsEq(a0,a2);AttrsEq(a0,a3);AttrsEq(a1,a2);AttrsEq(a1,a3);AttrsEq(a2,a3);AttrsEq(a4,a5);AttrsSub(a2,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a4,t2);AttrsSub(a5,t3);AttrsSub(a3,t2);NotNull(t1,a2);NotNull(t0,a1);NotNull(t0,a0);NotNull(t2,a4);NotNull(t3,a5);NotNull(t2,a3)",
//            "Proj*<a0>(InSubFilter<a1>(Input<t0>,Proj<a2>(Input<t1>)))|Proj*<a3>(InnerJoin<a4 a5>(Input<t2>,Input<t3>))|TableEq(t0,t1);TableEq(t0,t2);TableEq(t0,t3);TableEq(t1,t2);TableEq(t1,t3);TableEq(t2,t3);AttrsEq(a0,a3);AttrsEq(a0,a4);AttrsEq(a0,a5);AttrsEq(a1,a2);AttrsEq(a3,a4);AttrsEq(a3,a5);AttrsEq(a4,a5);AttrsSub(a2,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a4,t2);AttrsSub(a5,t3);AttrsSub(a3,t2);NotNull(t1,a2);NotNull(t0,a1);NotNull(t0,a0);NotNull(t2,a4);NotNull(t3,a5);NotNull(t2,a3)",
//            "Proj*<a0>(InSubFilter<a1>(Input<t0>,Proj<a2>(Input<t1>)))|Proj*<a3>(InnerJoin<a4 a5>(Input<t2>,Input<t3>))|TableEq(t0,t1);TableEq(t0,t2);TableEq(t0,t3);TableEq(t1,t2);TableEq(t1,t3);TableEq(t2,t3);AttrsEq(a0,a3);AttrsEq(a0,a4);AttrsEq(a1,a2);AttrsEq(a1,a5);AttrsEq(a2,a5);AttrsEq(a3,a4);AttrsSub(a2,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a4,t2);AttrsSub(a5,t3);AttrsSub(a3,t2);NotNull(t1,a2);NotNull(t0,a1);NotNull(t0,a0);NotNull(t2,a4);NotNull(t3,a5);NotNull(t2,a3);Reference(t2,a4,t3,a5)",
//            "Proj*<a0>(InSubFilter<a1>(Input<t0>,Proj<a2>(Input<t1>)))|Proj*<a3>(InnerJoin<a4 a5>(Input<t2>,Input<t3>))|TableEq(t0,t1);TableEq(t0,t2);TableEq(t1,t2);AttrsEq(a0,a1);AttrsEq(a0,a2);AttrsEq(a0,a3);AttrsEq(a1,a2);AttrsEq(a1,a3);AttrsEq(a2,a3);AttrsSub(a2,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a4,t2);AttrsSub(a5,t3);AttrsSub(a3,t2);NotNull(t1,a2);NotNull(t0,a1);NotNull(t0,a0);NotNull(t2,a4);NotNull(t2,a3);Reference(t2,a4,t3,a5)",
//            "Proj*<a0>(InSubFilter<a1>(Input<t0>,Proj<a2>(Input<t1>)))|Proj*<a3>(InnerJoin<a4 a5>(Input<t2>,Input<t3>))|TableEq(t0,t1);TableEq(t0,t2);TableEq(t1,t2);AttrsEq(a0,a1);AttrsEq(a0,a2);AttrsEq(a0,a4);AttrsEq(a1,a2);AttrsEq(a1,a4);AttrsEq(a2,a4);AttrsEq(a3,a5);AttrsSub(a2,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a4,t2);AttrsSub(a5,t3);AttrsSub(a3,t3);Reference(t2,a4,t3,a5)",
//            "Proj*<a0>(InSubFilter<a1>(Input<t0>,Proj<a2>(Input<t1>)))|Proj*<a3>(InnerJoin<a4 a5>(Input<t2>,Input<t3>))|TableEq(t0,t1);TableEq(t0,t2);TableEq(t1,t2);AttrsEq(a0,a3);AttrsEq(a1,a2);AttrsEq(a1,a4);AttrsEq(a2,a4);AttrsSub(a2,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a4,t2);AttrsSub(a5,t3);AttrsSub(a3,t2);Reference(t2,a4,t3,a5)",
//            "Proj*<a0>(InSubFilter<a1>(Input<t0>,Proj<a2>(Input<t1>)))|Proj*<a3>(InnerJoin<a4 a5>(Input<t2>,Input<t3>))|TableEq(t0,t2);TableEq(t1,t3);AttrsEq(a0,a1);AttrsEq(a0,a4);AttrsEq(a1,a4);AttrsEq(a2,a3);AttrsEq(a2,a5);AttrsEq(a3,a5);AttrsSub(a2,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a4,t2);AttrsSub(a5,t3);AttrsSub(a3,t3)",
//            "Proj*<a0>(InSubFilter<a1>(Input<t0>,Proj<a2>(Input<t1>)))|Proj*<a3>(InnerJoin<a4 a5>(Input<t2>,Input<t3>))|TableEq(t0,t2);TableEq(t1,t3);AttrsEq(a0,a3);AttrsEq(a1,a4);AttrsEq(a2,a5);AttrsSub(a2,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a4,t2);AttrsSub(a5,t3);AttrsSub(a3,t2)",
//            "Proj*<a0>(InSubFilter<a1>(Input<t0>,Proj<a2>(Input<t1>)))|Proj*<a3>(InnerJoin<a4 a5>(Input<t2>,Input<t3>))|TableEq(t0,t3);TableEq(t1,t2);AttrsEq(a0,a1);AttrsEq(a0,a5);AttrsEq(a1,a5);AttrsEq(a2,a3);AttrsEq(a2,a4);AttrsEq(a3,a4);AttrsSub(a2,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a4,t2);AttrsSub(a5,t3);AttrsSub(a3,t2)",
//            "Proj*<a0>(InSubFilter<a1>(Input<t0>,Proj<a2>(Input<t1>)))|Proj*<a3>(InnerJoin<a4 a5>(Input<t2>,Input<t3>))|TableEq(t0,t3);TableEq(t1,t2);AttrsEq(a0,a3);AttrsEq(a1,a5);AttrsEq(a2,a4);AttrsSub(a2,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a4,t2);AttrsSub(a5,t3);AttrsSub(a3,t3)",
  }

  @Test
  void testPlainFilterCollapsing() {
    doTest(
        "PlainFilter(PlainFilter(Input))",
        "PlainFilter(Input)");
//        "Filter<p0 a0>(Filter<p1 a1>(Input<t0>))|Filter<p2 a2>(Input<t1>)|TableEq(t0,t1);AttrsEq(a0,a1);AttrsEq(a0,a2);AttrsEq(a1,a2);PredicateEq(p0,p1);PredicateEq(p0,p2);PredicateEq(p1,p2);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a2,t1)");
  }

  @Test
  void testINSubFilterCollapsing() {
    doTest(
        "InSubFilter(InSubFilter(Input,Input),Input)",
        "InSubFilter(Input,Input)");
//        "InSubFilter<a0>(InSubFilter<a1>(Input<t0>,Input<t1>),Input<t2>)|InSubFilter<a2>(Input<t3>,Input<t4>)|TableEq(t0,t3);TableEq(t1,t2);TableEq(t1,t4);TableEq(t2,t4);AttrsEq(a0,a1);AttrsEq(a0,a2);AttrsEq(a1,a2);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a2,t3)");
  }

  @Test
  void testProjCollapsing0() {
    doTest(
        "Proj(Proj(Input))",
        "Proj(Input)");
//        "Proj<a0>(Proj<a1>(Input<t0>))|Proj<a2>(Input<t1>)|TableEq(t0,t1);AttrsEq(a0,a2);AttrsSub(a1,t0);AttrsSub(a0,a1);AttrsSub(a2,t1)");
  }

  @Test
  void testInSubFilterElimination() {
    doTest(
        "InSubFilter(Input,Proj(Input))",
        "Input");
//        "InSubFilter<a0>(Input<t0>,Proj<a1>(Input<t1>))|Input<t2>|TableEq(t0,t1);TableEq(t0,t2);TableEq(t1,t2);AttrsEq(a0,a1);AttrsSub(a1,t1);AttrsSub(a0,t0);NotNull(t1,a1);NotNull(t0,a0)");
  }

  @Test
  void testRemoveDeduplication() {
    doTest(
        "Proj*(Input)",
        "Proj(Input)");
//        "Proj*<a0>(Input<t0>)|Proj<a1>(Input<t1>)|TableEq(t0,t1);AttrsEq(a0,a1);AttrsSub(a0,t0);AttrsSub(a1,t1);Unique(t0,a0);Unique(t1,a1)");
  }

//  @Test
  void testFlattenJoinSubquery() {
    doTest(
        "Proj(InnerJoin(Input,Proj(Filter(Input))))",
        "Proj(Filter(InnerJoin(Input,Input)))",
        "Proj*<a0>(Input<t0>)|Proj<a1>(Input<t1>)|TableEq(t0,t1);AttrsEq(a0,a1);AttrsSub(a0,t0);AttrsSub(a1,t1);Unique(t0,a0);Unique(t1,a1)");
  }

  @Test
  void testSubstituteAttr0() {
    doTest(
        "Filter(InnerJoin(Input,Input))",
        "Filter(InnerJoin(Input,Input))");
//            "Filter<p0 a0>(InnerJoin<a1 a2>(Input<t0>,Input<t1>))|Filter<p1 a3>(InnerJoin<a4 a5>(Input<t2>,Input<t3>))|TableEq(t0,t2);TableEq(t1,t3);AttrsEq(a0,a1);AttrsEq(a0,a4);AttrsEq(a1,a4);AttrsEq(a2,a3);AttrsEq(a2,a5);AttrsEq(a3,a5);PredicateEq(p0,p1);AttrsSub(a1,t0);AttrsSub(a2,t1);AttrsSub(a0,t0);AttrsSub(a4,t2);AttrsSub(a5,t3);AttrsSub(a3,t3)",
//            "Filter<p0 a0>(InnerJoin<a1 a2>(Input<t0>,Input<t1>))|Filter<p1 a3>(InnerJoin<a4 a5>(Input<t2>,Input<t3>))|TableEq(t0,t2);TableEq(t1,t3);AttrsEq(a0,a2);AttrsEq(a0,a5);AttrsEq(a1,a3);AttrsEq(a1,a4);AttrsEq(a2,a5);AttrsEq(a3,a4);PredicateEq(p0,p1);AttrsSub(a1,t0);AttrsSub(a2,t1);AttrsSub(a0,t1);AttrsSub(a4,t2);AttrsSub(a5,t3);AttrsSub(a3,t2)",
//            "Filter<p0 a0>(InnerJoin<a1 a2>(Input<t0>,Input<t1>))|Filter<p1 a3>(InnerJoin<a4 a5>(Input<t2>,Input<t3>))|TableEq(t0,t2);TableEq(t1,t3);AttrsEq(a0,a3);AttrsEq(a1,a4);AttrsEq(a2,a5);PredicateEq(p0,p1);AttrsSub(a1,t0);AttrsSub(a2,t1);AttrsSub(a0,t0);AttrsSub(a4,t2);AttrsSub(a5,t3);AttrsSub(a3,t2)",
//            "Filter<p0 a0>(InnerJoin<a1 a2>(Input<t0>,Input<t1>))|Filter<p1 a3>(InnerJoin<a4 a5>(Input<t2>,Input<t3>))|TableEq(t0,t2);TableEq(t1,t3);AttrsEq(a0,a3);AttrsEq(a1,a4);AttrsEq(a2,a5);PredicateEq(p0,p1);AttrsSub(a1,t0);AttrsSub(a2,t1);AttrsSub(a0,t1);AttrsSub(a4,t2);AttrsSub(a5,t3);AttrsSub(a3,t3)",
  }

  //  @Test
  void testSubstituteAttr1() {
    doTest(
        "Proj(InnerJoin(Input,Input))",
        "Proj(InnerJoin(Input,Input))",
        "Proj*<a0>(Input<t0>)|Proj<a1>(Input<t1>)|TableEq(t0,t1);AttrsEq(a0,a1);AttrsSub(a0,t0);AttrsSub(a1,t1);Unique(t0,a0);Unique(t1,a1)");
  }

  //  @Test
  void testSubstituteAttr2() {
    doTest(
        "Proj(Filter(InnerJoin(Input,Input)))",
        "Proj(Filter(InnerJoin(Input,Input)))",
        "Proj*<a0>(Input<t0>)|Proj<a1>(Input<t1>)|TableEq(t0,t1);AttrsEq(a0,a1);AttrsSub(a0,t0);AttrsSub(a1,t1);Unique(t0,a0);Unique(t1,a1)");
  }

  //  @Test
  void testSubstituteAttr3() {
    doTest(
        "InnerJoin(InnerJoin(Input,Input))",
        "InnerJoin(InnerJoin(Input,Input))",
        "Proj*<a0>(Input<t0>)|Proj<a1>(Input<t1>)|TableEq(t0,t1);AttrsEq(a0,a1);AttrsSub(a0,t0);AttrsSub(a1,t1);Unique(t0,a0);Unique(t1,a1)");
  }

  //  @Test
  void testSubstituteAttr4() {
    doTest(
        "LeftJoin(InnerJoin(Input,Input))",
        "LeftJoin(InnerJoin(Input,Input))",
        "Proj*<a0>(Input<t0>)|Proj<a1>(Input<t1>)|TableEq(t0,t1);AttrsEq(a0,a1);AttrsSub(a0,t0);AttrsSub(a1,t1);Unique(t0,a0);Unique(t1,a1)");
  }

  //  @Test
  void test() {
    final Substitution substitution0 =
        Substitution.parse(
            "Filter<p0 a0>(Proj*<a1>(InnerJoin<a2 a3>(Input<t0>,Input<t1>)))|Proj<a4>(Filter<p1 a5>(InnerJoin<a6 a7>(Input<t2>,Input<t3>)))|TableEq(t0,t2);TableEq(t1,t3);AttrsEq(a1,a3);AttrsEq(a1,a7);AttrsEq(a2,a4);AttrsEq(a2,a5);AttrsEq(a2,a6);AttrsEq(a3,a7);AttrsEq(a4,a5);AttrsEq(a4,a6);AttrsEq(a5,a6);PredicateEq(p0,p1);AttrsSub(a0,a1);AttrsSub(a1,t1);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a4,t2);AttrsSub(a5,t2);AttrsSub(a6,t2);AttrsSub(a7,t3);Unique(t0,a2);Unique(t1,a1);Unique(t1,a3);Unique(t2,a4);Unique(t2,a5);Unique(t2,a6);Unique(t3,a7)");
    final Substitution substitution = Substitution.parse(substitution0.canonicalStringify());
    final ConstraintEnumerator enumerator =
        mkConstraintEnumerator(substitution._0(), substitution._1(), mkLogicCtx());
    System.out.println(enumerator.prove(substitution.constraints()));
  }
}
