package sjtu.ipads.wtune.superopt;

import org.junit.jupiter.api.*;
import sjtu.ipads.wtune.common.utils.ListSupport;
import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.substitution.Substitution;

import java.util.List;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sjtu.ipads.wtune.superopt.constraint.ConstraintSupport.enumConstraints;

@Tag("slow")
@Tag("enumeration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestEnumeration {
  private static boolean echo = true;

  private static void doTest(String fragment0, String fragment1, String... expectation) {
    final Fragment f0 = Fragment.parse(fragment0, null);
    final Fragment f1 = Fragment.parse(fragment1, null);
    long startTime = System.currentTimeMillis();
    final List<Substitution> results = enumConstraints(f0, f1, -1);
    long endTime = System.currentTimeMillis();
    System.out.println("Execution time: " + (endTime - startTime) + "ms.");

    //    if (echo) results.forEach(TestEnumeration::printReadable);
    results.forEach(System.out::println);

    assertTrue(ListSupport.<Substitution, String>map((Iterable<Substitution>) results, (Function<? super Substitution, ? extends String>) Object::toString).containsAll(asList(expectation)));
  }

  @BeforeEach
  void init(TestInfo testInfo) {
    String methodName = testInfo.getTestMethod().orElseThrow().getName();
    System.out.println("----------" + methodName + "----------");
  }

  @Test
  @Order(1)
  void testInnerJoinElimination0() {
    doTest(
        "Proj(InnerJoin(Input,Input))", "Proj(Input)"
        //            "Proj<a0>(InnerJoin<a1
        // a2>(Input<t0>,Input<t1>))|Proj<a3>(Input<t2>)|TableEq(t0,t1);TableEq(t0,t2);TableEq(t1,t2);AttrsEq(a0,a3);AttrsEq(a1,a2);AttrsSub(a1,t0);AttrsSub(a2,t1);AttrsSub(a0,t0);AttrsSub(a3,t2);Unique(t0,a1);Unique(t1,a2);NotNull(t0,a1);NotNull(t1,a2)",
        //            "Proj<a0>(InnerJoin<a1
        // a2>(Input<t0>,Input<t1>))|Proj<a3>(Input<t2>)|TableEq(t0,t2);AttrsEq(a0,a3);AttrsSub(a1,t0);AttrsSub(a2,t1);AttrsSub(a0,t0);AttrsSub(a3,t2);Unique(t1,a2);NotNull(t0,a1);Reference(t0,a1,t1,a2)"
        );
  }

  @Test
  @Order(2)
  void testInnerJoinElimination1() {
    doTest("Proj*(InnerJoin(Input,Input))", "Proj*(Input)");
    //            "Proj*<a0>(InnerJoin<a1
    // a2>(Input<t0>,Input<t1>))|Proj*<a3>(Input<t2>)|TableEq(t0,t1);TableEq(t0,t2);TableEq(t1,t2);AttrsEq(a0,a3);AttrsEq(a1,a2);AttrsSub(a1,t0);AttrsSub(a2,t1);AttrsSub(a0,t0);AttrsSub(a3,t2);NotNull(t0,a1);NotNull(t1,a2)",
    //            "Proj*<a0>(InnerJoin<a1
    // a2>(Input<t0>,Input<t1>))|Proj*<a3>(Input<t2>)|TableEq(t0,t1);TableEq(t0,t2);TableEq(t1,t2);AttrsEq(a0,a3);AttrsEq(a1,a2);AttrsSub(a1,t0);AttrsSub(a2,t1);AttrsSub(a0,t1);AttrsSub(a3,t2);NotNull(t0,a1);NotNull(t1,a2)",
    //            "Proj*<a0>(InnerJoin<a1
    // a2>(Input<t0>,Input<t1>))|Proj*<a3>(Input<t2>)|TableEq(t0,t2);AttrsEq(a0,a2);AttrsEq(a1,a3);AttrsSub(a1,t0);AttrsSub(a2,t1);AttrsSub(a0,t1);AttrsSub(a3,t2);NotNull(t0,a1);NotNull(t2,a3);Reference(t0,a1,t1,a2)",
    //            "Proj*<a0>(InnerJoin<a1
    // a2>(Input<t0>,Input<t1>))|Proj*<a3>(Input<t2>)|TableEq(t0,t2);AttrsEq(a0,a3);AttrsSub(a1,t0);AttrsSub(a2,t1);AttrsSub(a0,t0);AttrsSub(a3,t2);NotNull(t0,a1);Reference(t0,a1,t1,a2)"
  }

  @Test
  @Order(3)
  void testInnerJoinElimination2() {
    doTest("Proj(PlainFilter(InnerJoin(Input,Input)))", "Proj(PlainFilter(Input))");
    //            "Proj<a0>(Filter<p0 a1>(InnerJoin<a2 a3>(Input<t0>,Input<t1>)))|Proj<a4>(Filter<p1
    // a5>(Input<t2>))|TableEq(t0,t1);TableEq(t0,t2);TableEq(t1,t2);AttrsEq(a0,a4);AttrsEq(a1,a5);AttrsEq(a2,a3);PredicateEq(p0,p1);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a5,t2);AttrsSub(a4,t2);Unique(t0,a2);Unique(t1,a3);NotNull(t0,a2);NotNull(t1,a3)",
    //            "Proj<a0>(Filter<p0 a1>(InnerJoin<a2 a3>(Input<t0>,Input<t1>)))|Proj<a4>(Filter<p1
    // a5>(Input<t2>))|TableEq(t0,t2);AttrsEq(a0,a4);AttrsEq(a1,a3);AttrsEq(a2,a5);PredicateEq(p0,p1);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a1,t1);AttrsSub(a0,t0);AttrsSub(a5,t2);AttrsSub(a4,t2);Unique(t1,a3);Unique(t1,a1);NotNull(t0,a2);NotNull(t2,a5);Reference(t0,a2,t1,a3)",
    //            "Proj<a0>(Filter<p0 a1>(InnerJoin<a2 a3>(Input<t0>,Input<t1>)))|Proj<a4>(Filter<p1
    // a5>(Input<t2>))|TableEq(t0,t2);AttrsEq(a0,a4);AttrsEq(a1,a5);PredicateEq(p0,p1);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a5,t2);AttrsSub(a4,t2);Unique(t1,a3);NotNull(t0,a2);Reference(t0,a2,t1,a3)",
  }

  @Test
  @Order(4)
  void testInnerJoinElimination3() {
    doTest("Proj*(PlainFilter(InnerJoin(Input,Input)))", "Proj*(PlainFilter(Input))");
    //            "Proj*<a0>(Filter<p0 a1>(InnerJoin<a2
    // a3>(Input<t0>,Input<t1>)))|Proj*<a4>(Filter<p1
    // a5>(Input<t2>))|TableEq(t0,t1);TableEq(t0,t2);TableEq(t1,t2);AttrsEq(a0,a4);AttrsEq(a1,a5);AttrsEq(a2,a3);PredicateEq(p0,p1);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a5,t2);AttrsSub(a4,t2);NotNull(t0,a2);NotNull(t1,a3)",
    //            "Proj*<a0>(Filter<p0 a1>(InnerJoin<a2
    // a3>(Input<t0>,Input<t1>)))|Proj*<a4>(Filter<p1
    // a5>(Input<t2>))|TableEq(t0,t2);AttrsEq(a0,a1);AttrsEq(a0,a3);AttrsEq(a1,a3);AttrsEq(a2,a4);AttrsEq(a2,a5);AttrsEq(a4,a5);PredicateEq(p0,p1);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a1,t1);AttrsSub(a0,t1);AttrsSub(a5,t2);AttrsSub(a4,t2);NotNull(t0,a2);NotNull(t2,a5);NotNull(t2,a4);Reference(t0,a2,t1,a3)",
    //            "Proj*<a0>(Filter<p0 a1>(InnerJoin<a2
    // a3>(Input<t0>,Input<t1>)))|Proj*<a4>(Filter<p1
    // a5>(Input<t2>))|TableEq(t0,t2);AttrsEq(a0,a3);AttrsEq(a1,a5);AttrsEq(a2,a4);PredicateEq(p0,p1);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a1,t0);AttrsSub(a0,t1);AttrsSub(a5,t2);AttrsSub(a4,t2);NotNull(t0,a2);NotNull(t2,a4);Reference(t0,a2,t1,a3)",
    //            "Proj*<a0>(Filter<p0 a1>(InnerJoin<a2
    // a3>(Input<t0>,Input<t1>)))|Proj*<a4>(Filter<p1
    // a5>(Input<t2>))|TableEq(t0,t2);AttrsEq(a0,a4);AttrsEq(a1,a3);AttrsEq(a2,a5);PredicateEq(p0,p1);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a1,t1);AttrsSub(a0,t0);AttrsSub(a5,t2);AttrsSub(a4,t2);NotNull(t0,a2);NotNull(t2,a5);Reference(t0,a2,t1,a3)",
    //            "Proj*<a0>(Filter<p0 a1>(InnerJoin<a2
    // a3>(Input<t0>,Input<t1>)))|Proj*<a4>(Filter<p1
    // a5>(Input<t2>))|TableEq(t0,t2);AttrsEq(a0,a4);AttrsEq(a1,a5);PredicateEq(p0,p1);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a5,t2);AttrsSub(a4,t2);NotNull(t0,a2);Reference(t0,a2,t1,a3)",
  }

  @Test
  @Order(5)
  void testLeftJoinElimination0() {
    doTest("Proj(LeftJoin(Input,Input))", "Proj(Input)");
    //            "Proj<a0>(LeftJoin<a1
    // a2>(Input<t0>,Input<t1>))|Proj<a3>(Input<t2>)|TableEq(t0,t1);TableEq(t0,t2);TableEq(t1,t2);AttrsEq(a0,a3);AttrsSub(a1,t0);AttrsSub(a2,t1);AttrsSub(a0,t0);AttrsSub(a3,t2);Unique(t1,a2)",
    //            "Proj<a0>(LeftJoin<a1
    // a2>(Input<t0>,Input<t1>))|Proj<a3>(Input<t2>)|TableEq(t0,t2);AttrsEq(a0,a1);AttrsEq(a0,a3);AttrsEq(a1,a3);AttrsSub(a1,t0);AttrsSub(a2,t1);AttrsSub(a0,t0);AttrsSub(a3,t2);Unique(t1,a2)",
    //            "Proj<a0>(LeftJoin<a1
    // a2>(Input<t0>,Input<t1>))|Proj<a3>(Input<t2>)|TableEq(t0,t2);AttrsEq(a0,a3);AttrsSub(a1,t0);AttrsSub(a2,t1);AttrsSub(a0,t0);AttrsSub(a3,t2);Unique(t1,a2);NotNull(t0,a1)",
  }

  @Test
  @Order(6)
  void testLeftJoinElimination1() {
    doTest("Proj*(LeftJoin(Input,Input))", "Proj*(Input)");
    //            "Proj*<a0>(LeftJoin<a1
    // a2>(Input<t0>,Input<t1>))|Proj*<a3>(Input<t2>)|TableEq(t0,t1);TableEq(t0,t2);TableEq(t1,t2);AttrsEq(a0,a1);AttrsEq(a0,a2);AttrsEq(a0,a3);AttrsEq(a1,a2);AttrsEq(a1,a3);AttrsEq(a2,a3);AttrsSub(a1,t0);AttrsSub(a2,t1);AttrsSub(a0,t1);AttrsSub(a3,t2)",
    //            "Proj*<a0>(LeftJoin<a1
    // a2>(Input<t0>,Input<t1>))|Proj*<a3>(Input<t2>)|TableEq(t0,t1);TableEq(t0,t2);TableEq(t1,t2);AttrsEq(a0,a3);AttrsEq(a1,a2);AttrsSub(a1,t0);AttrsSub(a2,t1);AttrsSub(a0,t1);AttrsSub(a3,t2);NotNull(t0,a1);NotNull(t1,a2)",
    //            "Proj*<a0>(LeftJoin<a1
    // a2>(Input<t0>,Input<t1>))|Proj*<a3>(Input<t2>)|TableEq(t0,t2);AttrsEq(a0,a2);AttrsEq(a1,a3);AttrsSub(a1,t0);AttrsSub(a2,t1);AttrsSub(a0,t1);AttrsSub(a3,t2);Reference(t0,a1,t1,a2)",
    //            "Proj*<a0>(LeftJoin<a1
    // a2>(Input<t0>,Input<t1>))|Proj*<a3>(Input<t2>)|TableEq(t0,t2);AttrsEq(a0,a3);AttrsSub(a1,t0);AttrsSub(a2,t1);AttrsSub(a0,t0);AttrsSub(a3,t2)",
  }

  @Test
  @Order(7)
  void testLeftJoinElimination2() {
    doTest("Proj(Filter(LeftJoin(Input,Input)))", "Proj(Filter(Input))");
    //            "Proj<a0>(Filter<p0 a1>(LeftJoin<a2 a3>(Input<t0>,Input<t1>)))|Proj<a4>(Filter<p1
    // a5>(Input<t2>))|TableEq(t0,t1);TableEq(t0,t2);TableEq(t1,t2);AttrsEq(a0,a4);AttrsEq(a1,a5);PredicateEq(p0,p1);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a5,t2);AttrsSub(a4,t2);Unique(t1,a3)",
    //            "Proj<a0>(Filter<p0 a1>(LeftJoin<a2 a3>(Input<t0>,Input<t1>)))|Proj<a4>(Filter<p1
    // a5>(Input<t2>))|TableEq(t0,t2);AttrsEq(a0,a2);AttrsEq(a0,a4);AttrsEq(a0,a5);AttrsEq(a1,a3);AttrsEq(a2,a4);AttrsEq(a2,a5);AttrsEq(a4,a5);PredicateEq(p0,p1);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a1,t1);AttrsSub(a0,t0);AttrsSub(a5,t2);AttrsSub(a4,t2);Unique(t1,a3);Unique(t1,a1);Reference(t0,a2,t1,a3)",
    //            "Proj<a0>(Filter<p0 a1>(LeftJoin<a2 a3>(Input<t0>,Input<t1>)))|Proj<a4>(Filter<p1
    // a5>(Input<t2>))|TableEq(t0,t2);AttrsEq(a0,a2);AttrsEq(a0,a4);AttrsEq(a1,a5);AttrsEq(a2,a4);PredicateEq(p0,p1);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a5,t2);AttrsSub(a4,t2);Unique(t1,a3)",
    //            "Proj<a0>(Filter<p0 a1>(LeftJoin<a2 a3>(Input<t0>,Input<t1>)))|Proj<a4>(Filter<p1
    // a5>(Input<t2>))|TableEq(t0,t2);AttrsEq(a0,a4);AttrsEq(a1,a3);AttrsEq(a2,a5);PredicateEq(p0,p1);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a1,t1);AttrsSub(a0,t0);AttrsSub(a5,t2);AttrsSub(a4,t2);Unique(t1,a3);Unique(t1,a1);NotNull(t0,a2);NotNull(t2,a5);Reference(t0,a2,t1,a3)",
    //            "Proj<a0>(Filter<p0 a1>(LeftJoin<a2 a3>(Input<t0>,Input<t1>)))|Proj<a4>(Filter<p1
    // a5>(Input<t2>))|TableEq(t0,t2);AttrsEq(a0,a4);AttrsEq(a1,a5);PredicateEq(p0,p1);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a5,t2);AttrsSub(a4,t2);Unique(t1,a3);NotNull(t0,a2)",
  }

  @Test
  @Order(8)
  void testLeftJoinElimination3() {
    doTest("Proj*(Filter(LeftJoin(Input,Input)))", "Proj*(Filter(Input))");
    //            "Proj*<a0>(Filter<p0 a1>(LeftJoin<a2
    // a3>(Input<t0>,Input<t1>)))|Proj*<a4>(Filter<p1
    // a5>(Input<t2>))|TableEq(t0,t1);TableEq(t0,t2);TableEq(t1,t2);AttrsEq(a0,a1);AttrsEq(a0,a2);AttrsEq(a0,a3);AttrsEq(a0,a4);AttrsEq(a0,a5);AttrsEq(a1,a2);AttrsEq(a1,a3);AttrsEq(a1,a4);AttrsEq(a1,a5);AttrsEq(a2,a3);AttrsEq(a2,a4);AttrsEq(a2,a5);AttrsEq(a3,a4);AttrsEq(a3,a5);AttrsEq(a4,a5);PredicateEq(p0,p1);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a1,t1);AttrsSub(a0,t1);AttrsSub(a5,t2);AttrsSub(a4,t2)",
    //            "Proj*<a0>(Filter<p0 a1>(LeftJoin<a2
    // a3>(Input<t0>,Input<t1>)))|Proj*<a4>(Filter<p1
    // a5>(Input<t2>))|TableEq(t0,t1);TableEq(t0,t2);TableEq(t1,t2);AttrsEq(a0,a2);AttrsEq(a0,a3);AttrsEq(a0,a4);AttrsEq(a1,a5);AttrsEq(a2,a3);AttrsEq(a2,a4);AttrsEq(a3,a4);PredicateEq(p0,p1);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a1,t0);AttrsSub(a0,t1);AttrsSub(a5,t2);AttrsSub(a4,t2)",
    //            "Proj*<a0>(Filter<p0 a1>(LeftJoin<a2
    // a3>(Input<t0>,Input<t1>)))|Proj*<a4>(Filter<p1
    // a5>(Input<t2>))|TableEq(t0,t1);TableEq(t0,t2);TableEq(t1,t2);AttrsEq(a0,a4);AttrsEq(a1,a2);AttrsEq(a1,a3);AttrsEq(a1,a5);AttrsEq(a2,a3);AttrsEq(a2,a5);AttrsEq(a3,a5);PredicateEq(p0,p1);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a1,t1);AttrsSub(a0,t0);AttrsSub(a5,t2);AttrsSub(a4,t2)",
    //            "Proj*<a0>(Filter<p0 a1>(LeftJoin<a2
    // a3>(Input<t0>,Input<t1>)))|Proj*<a4>(Filter<p1
    // a5>(Input<t2>))|TableEq(t0,t2);AttrsEq(a0,a1);AttrsEq(a0,a3);AttrsEq(a1,a3);AttrsEq(a2,a4);AttrsEq(a2,a5);AttrsEq(a4,a5);PredicateEq(p0,p1);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a1,t1);AttrsSub(a0,t1);AttrsSub(a5,t2);AttrsSub(a4,t2);Reference(t0,a2,t1,a3)",
    //            "Proj*<a0>(Filter<p0 a1>(LeftJoin<a2
    // a3>(Input<t0>,Input<t1>)))|Proj*<a4>(Filter<p1
    // a5>(Input<t2>))|TableEq(t0,t2);AttrsEq(a0,a3);AttrsEq(a1,a5);AttrsEq(a2,a4);PredicateEq(p0,p1);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a1,t0);AttrsSub(a0,t1);AttrsSub(a5,t2);AttrsSub(a4,t2);Reference(t0,a2,t1,a3)",
    //            "Proj*<a0>(Filter<p0 a1>(LeftJoin<a2
    // a3>(Input<t0>,Input<t1>)))|Proj*<a4>(Filter<p1
    // a5>(Input<t2>))|TableEq(t0,t2);AttrsEq(a0,a4);AttrsEq(a1,a3);AttrsEq(a2,a5);PredicateEq(p0,p1);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a1,t1);AttrsSub(a0,t0);AttrsSub(a5,t2);AttrsSub(a4,t2);Reference(t0,a2,t1,a3)",
    //            "Proj*<a0>(Filter<p0 a1>(LeftJoin<a2
    // a3>(Input<t0>,Input<t1>)))|Proj*<a4>(Filter<p1
    // a5>(Input<t2>))|TableEq(t0,t2);AttrsEq(a0,a4);AttrsEq(a1,a5);PredicateEq(p0,p1);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a5,t2);AttrsSub(a4,t2)",
  }

  @Test
  @Order(9)
  void testLeftJoin2InnerJoin() {
    doTest("LeftJoin(Input,Input)", "InnerJoin(Input,Input)");
    //            "LeftJoin<a0 a1>(Input<t0>,Input<t1>)|InnerJoin<a2
    // a3>(Input<t2>,Input<t3>)|TableEq(t0,t1);TableEq(t0,t2);TableEq(t0,t3);TableEq(t1,t2);TableEq(t1,t3);TableEq(t2,t3);AttrsEq(a0,a1);AttrsEq(a0,a2);AttrsEq(a0,a3);AttrsEq(a1,a2);AttrsEq(a1,a3);AttrsEq(a2,a3);AttrsSub(a0,t0);AttrsSub(a1,t1);AttrsSub(a2,t2);AttrsSub(a3,t3);NotNull(t0,a0);NotNull(t1,a1);NotNull(t2,a2);NotNull(t3,a3)",
    //            "LeftJoin<a0 a1>(Input<t0>,Input<t1>)|InnerJoin<a2
    // a3>(Input<t2>,Input<t3>)|TableEq(t0,t2);TableEq(t1,t3);AttrsEq(a0,a2);AttrsEq(a1,a3);AttrsSub(a0,t0);AttrsSub(a1,t1);AttrsSub(a2,t2);AttrsSub(a3,t3);NotNull(t0,a0);NotNull(t2,a2);Reference(t0,a0,t1,a1);Reference(t2,a2,t3,a3)",
  }

  @Test
  @Order(10)
  void testIN2InnerJoin0() {
    doTest("Proj(InSubFilter(Input,Proj(Input)))", "Proj(InnerJoin(Input,Input))");
    //            "Proj<a0>(InSubFilter<a1>(Input<t0>,Proj<a2>(Input<t1>)))|Proj<a3>(InnerJoin<a4
    // a5>(Input<t2>,Input<t3>))|TableEq(t0,t1);TableEq(t0,t2);TableEq(t0,t3);TableEq(t1,t2);TableEq(t1,t3);TableEq(t2,t3);AttrsEq(a0,a1);AttrsEq(a0,a2);AttrsEq(a0,a3);AttrsEq(a1,a2);AttrsEq(a1,a3);AttrsEq(a2,a3);AttrsEq(a4,a5);AttrsSub(a2,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a4,t2);AttrsSub(a5,t3);AttrsSub(a3,t2);Unique(t2,a4);Unique(t3,a5);NotNull(t1,a2);NotNull(t0,a1);NotNull(t0,a0);NotNull(t2,a4);NotNull(t3,a5);NotNull(t2,a3)",
    //            "Proj<a0>(InSubFilter<a1>(Input<t0>,Proj<a2>(Input<t1>)))|Proj<a3>(InnerJoin<a4
    // a5>(Input<t2>,Input<t3>))|TableEq(t0,t1);TableEq(t0,t2);TableEq(t0,t3);TableEq(t1,t2);TableEq(t1,t3);TableEq(t2,t3);AttrsEq(a0,a3);AttrsEq(a0,a4);AttrsEq(a0,a5);AttrsEq(a1,a2);AttrsEq(a3,a4);AttrsEq(a3,a5);AttrsEq(a4,a5);AttrsSub(a2,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a4,t2);AttrsSub(a5,t3);AttrsSub(a3,t2);Unique(t0,a0);Unique(t2,a4);Unique(t3,a5);Unique(t2,a3);NotNull(t1,a2);NotNull(t0,a1);NotNull(t0,a0);NotNull(t2,a4);NotNull(t3,a5);NotNull(t2,a3)",
    //            "Proj<a0>(InSubFilter<a1>(Input<t0>,Proj<a2>(Input<t1>)))|Proj<a3>(InnerJoin<a4
    // a5>(Input<t2>,Input<t3>))|TableEq(t0,t1);TableEq(t0,t2);TableEq(t0,t3);TableEq(t1,t2);TableEq(t1,t3);TableEq(t2,t3);AttrsEq(a0,a3);AttrsEq(a0,a4);AttrsEq(a1,a2);AttrsEq(a1,a5);AttrsEq(a2,a5);AttrsEq(a3,a4);AttrsSub(a2,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a4,t2);AttrsSub(a5,t3);AttrsSub(a3,t2);Unique(t1,a2);Unique(t0,a1);Unique(t3,a5);NotNull(t1,a2);NotNull(t0,a1);NotNull(t0,a0);NotNull(t2,a4);NotNull(t3,a5);NotNull(t2,a3);Reference(t2,a4,t3,a5)",
    //            "Proj<a0>(InSubFilter<a1>(Input<t0>,Proj<a2>(Input<t1>)))|Proj<a3>(InnerJoin<a4
    // a5>(Input<t2>,Input<t3>))|TableEq(t0,t1);TableEq(t0,t2);TableEq(t1,t2);AttrsEq(a0,a1);AttrsEq(a0,a2);AttrsEq(a0,a3);AttrsEq(a1,a2);AttrsEq(a1,a3);AttrsEq(a2,a3);AttrsSub(a2,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a4,t2);AttrsSub(a5,t3);AttrsSub(a3,t2);Unique(t3,a5);NotNull(t1,a2);NotNull(t0,a1);NotNull(t0,a0);NotNull(t2,a4);NotNull(t2,a3);Reference(t2,a4,t3,a5)",
    //            "Proj<a0>(InSubFilter<a1>(Input<t0>,Proj<a2>(Input<t1>)))|Proj<a3>(InnerJoin<a4
    // a5>(Input<t2>,Input<t3>))|TableEq(t0,t1);TableEq(t0,t2);TableEq(t1,t2);AttrsEq(a0,a3);AttrsEq(a1,a2);AttrsEq(a1,a4);AttrsEq(a2,a4);AttrsSub(a2,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a4,t2);AttrsSub(a5,t3);AttrsSub(a3,t2);Unique(t3,a5);Reference(t2,a4,t3,a5)",
    //            "Proj<a0>(InSubFilter<a1>(Input<t0>,Proj<a2>(Input<t1>)))|Proj<a3>(InnerJoin<a4
    // a5>(Input<t2>,Input<t3>))|TableEq(t0,t2);TableEq(t1,t3);AttrsEq(a0,a3);AttrsEq(a1,a4);AttrsEq(a2,a5);AttrsSub(a2,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a4,t2);AttrsSub(a5,t3);AttrsSub(a3,t2);Unique(t1,a2);Unique(t3,a5)",
    //            "Proj<a0>(InSubFilter<a1>(Input<t0>,Proj<a2>(Input<t1>)))|Proj<a3>(InnerJoin<a4
    // a5>(Input<t2>,Input<t3>))|TableEq(t0,t3);TableEq(t1,t2);AttrsEq(a0,a1);AttrsEq(a0,a5);AttrsEq(a1,a5);AttrsEq(a2,a3);AttrsEq(a2,a4);AttrsEq(a3,a4);AttrsSub(a2,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a4,t2);AttrsSub(a5,t3);AttrsSub(a3,t2);Unique(t1,a2);Unique(t2,a4);Unique(t2,a3)",
  }

  @Test
  @Order(11)
  void testIN2InnerJoin1() {
    doTest("Proj*(InSubFilter(Input,Proj(Input)))", "Proj*(InnerJoin(Input,Input))");
    //            "Proj*<a0>(InSubFilter<a1>(Input<t0>,Proj<a2>(Input<t1>)))|Proj*<a3>(InnerJoin<a4
    // a5>(Input<t2>,Input<t3>))|TableEq(t0,t1);TableEq(t0,t2);TableEq(t0,t3);TableEq(t1,t2);TableEq(t1,t3);TableEq(t2,t3);AttrsEq(a0,a1);AttrsEq(a0,a2);AttrsEq(a0,a3);AttrsEq(a1,a2);AttrsEq(a1,a3);AttrsEq(a2,a3);AttrsEq(a4,a5);AttrsSub(a2,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a4,t2);AttrsSub(a5,t3);AttrsSub(a3,t2);NotNull(t1,a2);NotNull(t0,a1);NotNull(t0,a0);NotNull(t2,a4);NotNull(t3,a5);NotNull(t2,a3)",
    //            "Proj*<a0>(InSubFilter<a1>(Input<t0>,Proj<a2>(Input<t1>)))|Proj*<a3>(InnerJoin<a4
    // a5>(Input<t2>,Input<t3>))|TableEq(t0,t1);TableEq(t0,t2);TableEq(t0,t3);TableEq(t1,t2);TableEq(t1,t3);TableEq(t2,t3);AttrsEq(a0,a3);AttrsEq(a0,a4);AttrsEq(a0,a5);AttrsEq(a1,a2);AttrsEq(a3,a4);AttrsEq(a3,a5);AttrsEq(a4,a5);AttrsSub(a2,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a4,t2);AttrsSub(a5,t3);AttrsSub(a3,t2);NotNull(t1,a2);NotNull(t0,a1);NotNull(t0,a0);NotNull(t2,a4);NotNull(t3,a5);NotNull(t2,a3)",
    //            "Proj*<a0>(InSubFilter<a1>(Input<t0>,Proj<a2>(Input<t1>)))|Proj*<a3>(InnerJoin<a4
    // a5>(Input<t2>,Input<t3>))|TableEq(t0,t1);TableEq(t0,t2);TableEq(t0,t3);TableEq(t1,t2);TableEq(t1,t3);TableEq(t2,t3);AttrsEq(a0,a3);AttrsEq(a0,a4);AttrsEq(a1,a2);AttrsEq(a1,a5);AttrsEq(a2,a5);AttrsEq(a3,a4);AttrsSub(a2,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a4,t2);AttrsSub(a5,t3);AttrsSub(a3,t2);NotNull(t1,a2);NotNull(t0,a1);NotNull(t0,a0);NotNull(t2,a4);NotNull(t3,a5);NotNull(t2,a3);Reference(t2,a4,t3,a5)",
    //            "Proj*<a0>(InSubFilter<a1>(Input<t0>,Proj<a2>(Input<t1>)))|Proj*<a3>(InnerJoin<a4
    // a5>(Input<t2>,Input<t3>))|TableEq(t0,t1);TableEq(t0,t2);TableEq(t1,t2);AttrsEq(a0,a1);AttrsEq(a0,a2);AttrsEq(a0,a3);AttrsEq(a1,a2);AttrsEq(a1,a3);AttrsEq(a2,a3);AttrsSub(a2,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a4,t2);AttrsSub(a5,t3);AttrsSub(a3,t2);NotNull(t1,a2);NotNull(t0,a1);NotNull(t0,a0);NotNull(t2,a4);NotNull(t2,a3);Reference(t2,a4,t3,a5)",
    //            "Proj*<a0>(InSubFilter<a1>(Input<t0>,Proj<a2>(Input<t1>)))|Proj*<a3>(InnerJoin<a4
    // a5>(Input<t2>,Input<t3>))|TableEq(t0,t1);TableEq(t0,t2);TableEq(t1,t2);AttrsEq(a0,a1);AttrsEq(a0,a2);AttrsEq(a0,a4);AttrsEq(a1,a2);AttrsEq(a1,a4);AttrsEq(a2,a4);AttrsEq(a3,a5);AttrsSub(a2,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a4,t2);AttrsSub(a5,t3);AttrsSub(a3,t3);Reference(t2,a4,t3,a5)",
    //            "Proj*<a0>(InSubFilter<a1>(Input<t0>,Proj<a2>(Input<t1>)))|Proj*<a3>(InnerJoin<a4
    // a5>(Input<t2>,Input<t3>))|TableEq(t0,t1);TableEq(t0,t2);TableEq(t1,t2);AttrsEq(a0,a3);AttrsEq(a1,a2);AttrsEq(a1,a4);AttrsEq(a2,a4);AttrsSub(a2,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a4,t2);AttrsSub(a5,t3);AttrsSub(a3,t2);Reference(t2,a4,t3,a5)",
    //            "Proj*<a0>(InSubFilter<a1>(Input<t0>,Proj<a2>(Input<t1>)))|Proj*<a3>(InnerJoin<a4
    // a5>(Input<t2>,Input<t3>))|TableEq(t0,t2);TableEq(t1,t3);AttrsEq(a0,a1);AttrsEq(a0,a4);AttrsEq(a1,a4);AttrsEq(a2,a3);AttrsEq(a2,a5);AttrsEq(a3,a5);AttrsSub(a2,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a4,t2);AttrsSub(a5,t3);AttrsSub(a3,t3)",
    //            "Proj*<a0>(InSubFilter<a1>(Input<t0>,Proj<a2>(Input<t1>)))|Proj*<a3>(InnerJoin<a4
    // a5>(Input<t2>,Input<t3>))|TableEq(t0,t2);TableEq(t1,t3);AttrsEq(a0,a3);AttrsEq(a1,a4);AttrsEq(a2,a5);AttrsSub(a2,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a4,t2);AttrsSub(a5,t3);AttrsSub(a3,t2)",
    //            "Proj*<a0>(InSubFilter<a1>(Input<t0>,Proj<a2>(Input<t1>)))|Proj*<a3>(InnerJoin<a4
    // a5>(Input<t2>,Input<t3>))|TableEq(t0,t3);TableEq(t1,t2);AttrsEq(a0,a1);AttrsEq(a0,a5);AttrsEq(a1,a5);AttrsEq(a2,a3);AttrsEq(a2,a4);AttrsEq(a3,a4);AttrsSub(a2,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a4,t2);AttrsSub(a5,t3);AttrsSub(a3,t2)",
    //            "Proj*<a0>(InSubFilter<a1>(Input<t0>,Proj<a2>(Input<t1>)))|Proj*<a3>(InnerJoin<a4
    // a5>(Input<t2>,Input<t3>))|TableEq(t0,t3);TableEq(t1,t2);AttrsEq(a0,a3);AttrsEq(a1,a5);AttrsEq(a2,a4);AttrsSub(a2,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a4,t2);AttrsSub(a5,t3);AttrsSub(a3,t3)",
  }

  @Test
  @Order(12)
  void testPlainFilterCollapsing() {
    doTest("PlainFilter(PlainFilter(Input))", "PlainFilter(Input)");
    //        "Filter<p0 a0>(Filter<p1 a1>(Input<t0>))|Filter<p2
    // a2>(Input<t1>)|TableEq(t0,t1);AttrsEq(a0,a1);AttrsEq(a0,a2);AttrsEq(a1,a2);PredicateEq(p0,p1);PredicateEq(p0,p2);PredicateEq(p1,p2);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a2,t1)");
  }

  @Test
  @Order(13)
  void testINSubFilterCollapsing() {
    doTest("InSubFilter(InSubFilter(Input,Input),Input)", "InSubFilter(Input,Input)");
    //
    // "InSubFilter<a0>(InSubFilter<a1>(Input<t0>,Input<t1>),Input<t2>)|InSubFilter<a2>(Input<t3>,Input<t4>)|TableEq(t0,t3);TableEq(t1,t2);TableEq(t1,t4);TableEq(t2,t4);AttrsEq(a0,a1);AttrsEq(a0,a2);AttrsEq(a1,a2);AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a2,t3)");
  }

  @Test
  @Order(14)
  void testProjCollapsing0() {
    doTest("Proj(Proj(Input))", "Proj(Input)");
    //
    // "Proj<a0>(Proj<a1>(Input<t0>))|Proj<a2>(Input<t1>)|TableEq(t0,t1);AttrsEq(a0,a2);AttrsSub(a1,t0);AttrsSub(a0,a1);AttrsSub(a2,t1)");
  }

  @Test
  @Order(15)
  void testInSubFilterElimination() {
    doTest("InSubFilter(Input,Proj(Input))", "Input");
    //
    // "InSubFilter<a0>(Input<t0>,Proj<a1>(Input<t1>))|Input<t2>|TableEq(t0,t1);TableEq(t0,t2);TableEq(t1,t2);AttrsEq(a0,a1);AttrsSub(a1,t1);AttrsSub(a0,t0);NotNull(t1,a1);NotNull(t0,a0)");
  }

  @Test
  @Order(16)
  void testRemoveDeduplication() {
    doTest("Proj*(Input)", "Proj(Input)");
    //
    // "Proj*<a0>(Input<t0>)|Proj<a1>(Input<t1>)|TableEq(t0,t1);AttrsEq(a0,a1);AttrsSub(a0,t0);AttrsSub(a1,t1);Unique(t0,a0);Unique(t1,a1)");
  }

  //  @Test
  void testFlattenJoinSubquery() {
    doTest(
        "Proj(InnerJoin(Input,Proj(Filter(Input))))",
        "Proj(Filter(InnerJoin(Input,Input)))",
        "Proj*<a0>(Input<t0>)|Proj<a1>(Input<t1>)|TableEq(t0,t1);AttrsEq(a0,a1);AttrsSub(a0,t0);AttrsSub(a1,t1);Unique(t0,a0);Unique(t1,a1)");
  }

  @Test
  @Order(17)
  void testSubstituteAttr0() {
    doTest("Filter(InnerJoin(Input,Input))", "Filter(InnerJoin(Input,Input))");
    //            "Filter<p0 a0>(InnerJoin<a1 a2>(Input<t0>,Input<t1>))|Filter<p1 a3>(InnerJoin<a4
    // a5>(Input<t2>,Input<t3>))|TableEq(t0,t2);TableEq(t1,t3);AttrsEq(a0,a1);AttrsEq(a0,a4);AttrsEq(a1,a4);AttrsEq(a2,a3);AttrsEq(a2,a5);AttrsEq(a3,a5);PredicateEq(p0,p1);AttrsSub(a1,t0);AttrsSub(a2,t1);AttrsSub(a0,t0);AttrsSub(a4,t2);AttrsSub(a5,t3);AttrsSub(a3,t3)",
    //            "Filter<p0 a0>(InnerJoin<a1 a2>(Input<t0>,Input<t1>))|Filter<p1 a3>(InnerJoin<a4
    // a5>(Input<t2>,Input<t3>))|TableEq(t0,t2);TableEq(t1,t3);AttrsEq(a0,a2);AttrsEq(a0,a5);AttrsEq(a1,a3);AttrsEq(a1,a4);AttrsEq(a2,a5);AttrsEq(a3,a4);PredicateEq(p0,p1);AttrsSub(a1,t0);AttrsSub(a2,t1);AttrsSub(a0,t1);AttrsSub(a4,t2);AttrsSub(a5,t3);AttrsSub(a3,t2)",
    //            "Filter<p0 a0>(InnerJoin<a1 a2>(Input<t0>,Input<t1>))|Filter<p1 a3>(InnerJoin<a4
    // a5>(Input<t2>,Input<t3>))|TableEq(t0,t2);TableEq(t1,t3);AttrsEq(a0,a3);AttrsEq(a1,a4);AttrsEq(a2,a5);PredicateEq(p0,p1);AttrsSub(a1,t0);AttrsSub(a2,t1);AttrsSub(a0,t0);AttrsSub(a4,t2);AttrsSub(a5,t3);AttrsSub(a3,t2)",
    //            "Filter<p0 a0>(InnerJoin<a1 a2>(Input<t0>,Input<t1>))|Filter<p1 a3>(InnerJoin<a4
    // a5>(Input<t2>,Input<t3>))|TableEq(t0,t2);TableEq(t1,t3);AttrsEq(a0,a3);AttrsEq(a1,a4);AttrsEq(a2,a5);PredicateEq(p0,p1);AttrsSub(a1,t0);AttrsSub(a2,t1);AttrsSub(a0,t1);AttrsSub(a4,t2);AttrsSub(a5,t3);AttrsSub(a3,t3)",
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
}
