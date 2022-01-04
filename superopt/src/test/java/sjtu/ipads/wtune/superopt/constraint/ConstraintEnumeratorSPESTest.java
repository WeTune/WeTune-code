package sjtu.ipads.wtune.superopt.constraint;

import com.microsoft.z3.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import sjtu.ipads.wtune.spes.AlgeNode.AlgeNode;
import sjtu.ipads.wtune.spes.AlgeRule.AlgeRule;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.superopt.fragment.*;
import sjtu.ipads.wtune.superopt.logic.LogicSupport;
import sjtu.ipads.wtune.superopt.nodetrans.SPESSupport;
import sjtu.ipads.wtune.superopt.substitution.Substitution;
import sjtu.ipads.wtune.superopt.substitution.SubstitutionSupport;
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
      System.out.println(str);
      strings.add(str.split("\\|")[2]);
    }

    System.out.println(EnumerationMetrics.current());

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
  void testUnion1() {
    // Some pruning issues
    doTest(SPES,
        "Union*(Proj(Input),Proj(InnerJoin(Input,Input)))",
        "Union*(Proj(InnerJoin(Input,Input)),Proj(Input))");
  }

  @Test
  void testAgg0() {
    final Substitution rule =
        Substitution.parse(
            "Agg<a0 a1 p0>(Input<t0>)|" +
                "Agg<a2 a3 p1>(Input<t1>)|" +
                "AttrsSub(a0,t0);AttrsSub(a1,t0);TableEq(t1,t0);AttrsEq(a2,a0);AttrsEq(a3,a1);PredicateEq(p1,p0)");
    var planPair = SubstitutionSupport.translateAsPlan2(rule);
    AlgeNode algeNode = SPESSupport.plan2AlgeNode(planPair.getLeft(), new Context());
    boolean res = SPESSupport.prove(planPair.getLeft(), planPair.getRight());
    System.out.println(planPair.getLeft().toString());
  }

  @Test
  void testAgg1() {
    doTest(SPES,
        "Agg(InnerJoin(Input,Input))",
        "Agg(InnerJoin(Input,Input))");
  }

  @Test
  void testAgg2() {
    // Agg{[`#`.`#` AS c0,COUNT(`#`.`#`)],group=[`#`.`#`],having=P1(COUNT(`#`.`#`)),refs=[q0.c0,q0.c0,q0.c0,q0.c0,]}
    // (Proj{[`#`.`#` AS c0,`#`.`#` AS c0,],refs=[q0.c0,q0.c0,],qual=q2}
    // (Filter{P0(`#`.`#`),refs=[q0.c0,]}
    // (Proj{[`#`.`#` AS c0,],refs=[t0.c0,],qual=q0}
    // (Input{r0 AS t0}))))
    final Substitution rule =
        Substitution.parse("Agg<a2 a3 p1>(Filter<p0 a1>(Proj<a0>(Input<t0>)))|" +
            "Agg<a5 a6 p3>(Filter<p2 a4>(Input<t1>))|" +
            "AttrsSub(a0,t0);AttrsSub(a1,a0);AttrsSub(a2,a0);AttrsSub(a3,a0);TableEq(t1,t0);AttrsEq(a4,a3);AttrsEq(a5,a2);AttrsEq(a6,a3);PredicateEq(p2,p1);PredicateEq(p3,p0)");
    var planPair = SubstitutionSupport.translateAsPlan2(rule);
    final Context ctx = new Context();
    AlgeNode algeNode0 = SPESSupport.plan2AlgeNode(planPair.getLeft(), ctx);
    AlgeNode algeNode1 = SPESSupport.plan2AlgeNode(planPair.getRight(), ctx);

    algeNode0 = AlgeRule.normalize(algeNode0);
    algeNode1 = AlgeRule.normalize(algeNode1);
    final int answer = LogicSupport.proveEqBySpes(rule);
    System.out.println(answer); // SPES: EQ
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
    // no rules, predicate null problem solved
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

  // ------------------------Test throwing exceptions-------------------------
  @Test
  void fragmentStatistic() {
    final List<Fragment> templates = FragmentSupportSPES.enumFragmentsSPES();
    final int numTemplates = templates.size();
    int unionCount = 0;
    for (Fragment fragment : templates) {
        if (fragment.root().kind() == OperatorType.SET_OP) unionCount++;
    }
    System.out.println("union count: " + unionCount);
    final int totalPairCount = (numTemplates * (numTemplates - 1)) >> 1;
    int enumPairCount = 0;
    for (int i = 0; i < numTemplates; ++i) {
      for (int j = i + 1; j < numTemplates; ++j) {
        final Fragment f0 = templates.get(i), f1 = templates.get(j);
        if (f0.symbolCount(Symbol.Kind.TABLE) != f1.symbolCount(Symbol.Kind.TABLE)) {
          enumPairCount++;
        }
      }
    }
    System.out.println("numTemplates: " + numTemplates);
    System.out.println("totalPairCount: " + totalPairCount);
    System.out.println("enumPairCount: " + enumPairCount);
  }

  @Test
  void exception0() {
    doTest(SPES, "Agg(Input)", "Agg(Filter(Proj*(Filter(Input))))");
  }

  @Test
  void exception1() {
    doTest(SPES, "Agg(Input)", "Agg(Proj*(Filter(Agg(Input))))");
  }
}
