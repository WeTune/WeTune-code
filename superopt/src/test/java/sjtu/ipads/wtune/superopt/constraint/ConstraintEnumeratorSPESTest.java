package sjtu.ipads.wtune.superopt.constraint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.fragment.SymbolNaming;
import sjtu.ipads.wtune.superopt.logic.LogicSupport;
import sjtu.ipads.wtune.superopt.substitution.Substitution;
import sjtu.ipads.wtune.superopt.uexpr.UExprSupport;
import sjtu.ipads.wtune.superopt.uexpr.UExprTranslationResult;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static sjtu.ipads.wtune.superopt.constraint.ConstraintSupport.ENUM_FLAG_USE_SPES;
import static sjtu.ipads.wtune.superopt.constraint.ConstraintSupport.enumConstraints2;

@Tag("enumeration")
public class ConstraintEnumeratorSPESTest {
  private static final String WeTune = "WeTune";
  private static final String SPES = "SPES";
  private static void doTest(String mode, String fragment0, String fragment1, String... expectations) {
    final Fragment f0 = Fragment.parse(fragment0, null);
    final Fragment f1 = Fragment.parse(fragment1, null);
    final SymbolNaming naming = SymbolNaming.mk();
    naming.name(f0.symbols());
    naming.name(f1.symbols());

    System.out.println("prover: " + mode);
    System.out.println(f0.stringify(naming));
    System.out.println(f1.stringify(naming));

    int flag = mode.equals(WeTune) ? 0 : ENUM_FLAG_USE_SPES;
    final List<Substitution> results = enumConstraints2(f0, f1, -1, flag, naming);
    final List<String> strings = new ArrayList<>(results.size());
    for (Substitution rule : results) {
      final String str = rule.toString();
      strings.add(str.split("\\|")[2]);
    }

    System.out.println(EnumerationMetrics.current());
    for (String string : strings) System.out.println(string);

    for (String expectation : expectations) {
      assertTrue(strings.contains(expectation), expectation);
    }
  }

  @BeforeEach
  void init(TestInfo testInfo) {
    String methodName = testInfo.getTestMethod().orElseThrow().getName();
    System.out.println("----------" + methodName + "----------");
  }

  @Test
  void testUnion() {
    doTest(SPES,
        "Union*(Proj(Input),Proj(Input))",
        "Union*(Proj(Input),Proj(Input))");
  }

  @Test
  void testIN2InnerJoin1() {
    // Pass
    doTest(SPES,
        "Proj*(InSubFilter(Input,Proj(Input)))",
        "Proj*(InnerJoin(Input,Input))",
        "AttrsSub(a0,t1);AttrsSub(a1,t0);AttrsSub(a2,t0);TableEq(t2,t0);TableEq(t3,t1);AttrsEq(a3,a1);AttrsEq(a4,a0);AttrsEq(a5,a2)");
  }

  @Test
  void testSingleRule() {
//    final Substitution rule =
//        Substitution.parse(
//            "Filter<p0 a0>(LeftJoin<a1 a2>(Input<t0>,Input<t1>))|" +
//                "InnerJoin<a3 a4>(Input<t2>,Filter<p1 a5>(Input<t3>))|" +
//                "AttrsEq(a0,a2);AttrsSub(a0,t1);AttrsSub(a1,t0);AttrsSub(a2,t1);" +
//                "TableEq(t2,t0);TableEq(t3,t1);AttrsEq(a3,a1);AttrsEq(a4,a2);AttrsEq(a5,a0);PredicateEq(p1,p0)");
//
//    final int answer = LogicSupport.proveEqBySpes(rule);
//    System.out.println(answer); // SPES: EQ
//
//    final UExprTranslationResult uExprs = UExprSupport.translateToUExpr(rule);
//    final int answer1 = LogicSupport.proveEq(uExprs);
//    System.out.println(answer1); // WeTune: NEQ

    final Substitution rule =
        Substitution.parse(
            "Proj<a1>(Proj<a0>(Input<t0>))|" +
            "Proj<a2>(Input<t1>)|" +
                "AttrsSub(a0,t0);AttrsSub(a1,a0);TableEq(t1,t0);AttrsEq(a2,a0)");

    final int answer = LogicSupport.proveEqBySpes(rule);
    System.out.println(answer); // SPES: EQ

    final UExprTranslationResult uExprs = UExprSupport.translateToUExpr(rule);
    final int answer1 = LogicSupport.proveEq(uExprs);
    System.out.println(answer1); // WeTune: NEQ
  }

  @Test
  void testPlainFilterCollapsing() {
    // Pass
    doTest(SPES,
        "PlainFilter(PlainFilter(Input))",
        "PlainFilter(Input)",
        "AttrsEq(a0,a1);PredicateEq(p0,p1);AttrsSub(a0,t0);AttrsSub(a1,t0);TableEq(t1,t0);AttrsEq(a2,a0);PredicateEq(p2,p0)");
  }

  @Test
  void testProjCollapsing0() {
    // Pass
    doTest(SPES,
        "Proj(Proj(Input))",
        "Proj(Input)",
        "AttrsSub(a0,t0);AttrsSub(a1,a0);TableEq(t1,t0);AttrsEq(a2,a1)");
  }

  @Test
  void testRemoveDeduplication() {
    // Pass, return NEQ
    doTest(SPES,
        "Proj*(Input)",
        "Proj(Input)");
//        "AttrsSub(a0,t0);Unique(t0,a0);TableEq(t1,t0);AttrsEq(a1,a0)");
  }

  // -----------------------Test case of SPES---------------------------------
  @Test
  void testJoinEqualOuterJoin0() {
    // Same result, both 4 rules, Pass
    doTest(SPES,
        "Filter(InnerJoin(Input,Input))",
        "InnerJoin(Input,Filter(Input))"
    );
    doTest(WeTune,
        "Filter(InnerJoin(Input,Input))",
        "InnerJoin(Input,Filter(Input))"
    );
  }

  @Test
  void testJoinEqualOuterJoin1() {
    // 1 rules, predicate null problem
    doTest(SPES,
        "Filter(LeftJoin(Input,Input))",
        "InnerJoin(Input,Filter(Input))"
    );
    // 2 rules with IC
    doTest(WeTune,
        "Filter(LeftJoin(Input,Input))",
        "InnerJoin(Input,Filter(Input))"
    );
  }
}
