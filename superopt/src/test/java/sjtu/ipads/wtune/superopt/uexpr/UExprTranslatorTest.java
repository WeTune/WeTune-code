package sjtu.ipads.wtune.superopt.uexpr;

import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.superopt.substitution.Substitution;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UExprTranslatorTest {
  @Test
  public void testLeftJoin0() {
    final Substitution rule =
        Substitution.parse(
            "Proj*<a1>(LeftJoin<a2 a3>(Input<t1>,Input<t2>)|"
                + "Proj*<a0>(Input<t0>)|"
                + "TableEq(t0,t1);AttrsEq(a0,a1);AttrsEq(a1,a2);"
                + "AttrsSub(a0,t0);AttrsSub(a2,t1);AttrsSub(a1,t1);AttrsSub(a3,t2)");
    final UExprTranslationResult result = UExprSupport.translateToUExpr(rule);
    assertEquals(3, result.symToTable.size());
    assertEquals(4, result.symToAttrs.size());
    assertEquals(8, result.varSchemas.size());
    assertEquals(4, result.symSchemas.size());
    System.out.println(result.srcExpr);
    System.out.println(result.tgtExpr);
    System.out.println(result.sourceFreeVar());
    System.out.println(result.targetFreeVar());
  }

  @Test
  public void testLeftJoin1() {
    final Substitution rule =
        Substitution.parse(
            "Proj<a1>(LeftJoin<a2 a3>(Input<t1>,Proj*<a4>(Input<t2>))|"
                + "Proj<a0>(Input<t0>)|"
                + "TableEq(t0,t1);AttrsEq(a0,a1);AttrsEq(a1,a2);"
                + "AttrsSub(a0,t0);AttrsSub(a2,t1);AttrsSub(a1,t1);AttrsSub(a3,a4);AttrsSub(a4,t2)");
    final UExprTranslationResult result = UExprSupport.translateToUExpr(rule);
    assertEquals(3, result.symToTable.size());
    assertEquals(5, result.symToAttrs.size());
    assertEquals(10, result.varSchemas.size());
    assertEquals(5, result.symSchemas.size());
    System.out.println(result.srcExpr);
    System.out.println(result.tgtExpr);
    System.out.println(result.sourceFreeVar());
    System.out.println(result.targetFreeVar());
  }

  @Test
  public void testSubquery0() {
    final Substitution rule =
        Substitution.parse(
            "Proj<a0>(InnerJoin<k0 k1>(Input<t0>,Input<t1>)|"
                + "Proj<a1>(InSubFilter<k2>(Input<t2>,Proj<k3>(Input<t3>))|"
                + "TableEq(t0,t2);TableEq(t1,t3);AttrsEq(a0,a1);AttrsEq(k0,k2);AttrsEq(k1,k3);"
                + "AttrsSub(a0,t0);AttrsSub(k0,t0);AttrsSub(k1,t1);"
                + "Unique(t1,k1)");
    final UExprTranslationResult result = UExprSupport.translateToUExpr(rule);
    assertEquals(4, result.symToTable.size());
    assertEquals(6, result.symToAttrs.size());
    assertEquals(8, result.varSchemas.size());
    assertEquals(5, result.symSchemas.size());
    System.out.println(result.srcExpr);
    System.out.println(result.tgtExpr);
    System.out.println(result.sourceFreeVar());
    System.out.println(result.targetFreeVar());
  }

  @Test
  public void testSubquery1() {
    final Substitution rule =
        Substitution.parse(
            "Proj<a0>(InnerJoin<k0 k1>(Input<t0>,Proj<k5>(Input<t1>)))|"
                + "Proj<a1>(InSubFilter<k2>(Input<t2>,Proj<k3>(Input<t3>))|"
                + "TableEq(t0,t2);TableEq(t1,t3);AttrsEq(a0,a1);AttrsEq(k0,k2);AttrsEq(k1,k5);AttrsEq(k3,k5);"
                + "AttrsSub(a0,t0);AttrsSub(k0,t0);AttrsSub(k1,k5);AttrsSub(k5,t1);"
                + "Unique(t1,k5)");
    final UExprTranslationResult result = UExprSupport.translateToUExpr(rule);
    assertEquals(4, result.symToTable.size());
    assertEquals(7, result.symToAttrs.size());
    assertEquals(9, result.varSchemas.size());
    assertEquals(6, result.symSchemas.size());
    System.out.println(result.srcExpr);
    System.out.println(result.tgtExpr);
    System.out.println(result.sourceFreeVar());
    System.out.println(result.targetFreeVar());
  }

  @Test
  public void testSubquery2() {
    final Substitution rule =
        Substitution.parse(
            "Proj<a0>(InnerJoin<k0 k1>(Input<t0>,Proj*<k5>(Input<t1>)))|"
                + "Input<t2>|"
                + "TableEq(t2,t0);"
                + "AttrsSub(a0,t0);AttrsSub(k0,t0);AttrsSub(k1,k5);AttrsSub(k5,t1);");
    final UExprTranslationResult result = UExprSupport.translateToUExpr(rule);
    assertEquals(3, result.symToTable.size());
    assertEquals(4, result.symToAttrs.size());
    assertEquals(7, result.varSchemas.size());
    assertEquals(4, result.symSchemas.size());
    System.out.println(result.srcExpr);
    System.out.println(result.tgtExpr);
    System.out.println(result.sourceFreeVar());
    System.out.println(result.targetFreeVar());
  }

  @Test
  public void testSubquery3() {
    final Substitution rule =
        Substitution.parse(
            "InSubFilter<k0>(Input<t0>,Proj<k1>(Input<t1>))|"
                + "Input<t2>|"
                + "TableEq(t2,t0);"
                + "AttrsSub(k0,t0);AttrsSub(k1,t1)");
    final UExprTranslationResult result = UExprSupport.translateToUExpr(rule);
    assertEquals(3, result.symToTable.size());
    assertEquals(2, result.symToAttrs.size());
    assertEquals(3, result.varSchemas.size());
    assertEquals(3, result.symSchemas.size());
    System.out.println(result.srcExpr);
    System.out.println(result.tgtExpr);
    System.out.println(result.sourceFreeVar());
    System.out.println(result.targetFreeVar());
  }

  @Test
  public void testProj() {
    final Substitution rule =
        Substitution.parse(
            "Proj<a0>(Proj<a1>(Input<t0>))|"
                + "Proj<a2>(Input<t1>)|"
                + "TableEq(t0,t1);AttrsEq(a0,a2);AttrsEq(a0,a1);"
                + "AttrsSub(a0,a1);AttrsSub(a1,t0)");
    final UExprTranslationResult result = UExprSupport.translateToUExpr(rule);
    assertEquals(2, result.symToTable.size());
    assertEquals(3, result.symToAttrs.size());
    assertEquals(6, result.varSchemas.size());
    assertEquals(4, result.symSchemas.size());
    System.out.println(result.srcExpr);
    System.out.println(result.tgtExpr);
    System.out.println(result.sourceFreeVar());
    System.out.println(result.targetFreeVar());
  }

  @Test
  public void testProjFilter() {
    final Substitution rule =
        Substitution.parse(
            "Filter<p0 a0>(Proj<a1>(Input<t0>))|"
                + "Proj<a2>(Filter<p1 a3>(Input<t1>)|"
                + "TableEq(t0,t1);AttrsEq(a3,a0);AttrsEq(a2,a1);PredicateEq(p1,p0);"
                + "AttrsSub(a0,a1);AttrsSub(a1,t0)");
    final UExprTranslationResult result = UExprSupport.translateToUExpr(rule);
    assertEquals(2, result.symToTable.size());
    assertEquals(4, result.symToAttrs.size());
    assertEquals(5, result.varSchemas.size());
    assertEquals(3, result.symSchemas.size());
    System.out.println(result.srcExpr);
    System.out.println(result.tgtExpr);
    System.out.println(result.sourceFreeVar());
    System.out.println(result.targetFreeVar());
  }
}
