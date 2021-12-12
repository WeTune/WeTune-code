package sjtu.ipads.wtune.superopt.logic;

import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.superopt.substitution.Substitution;
import sjtu.ipads.wtune.superopt.uexpr.UExprSupport;
import sjtu.ipads.wtune.superopt.uexpr.UExprTranslationResult;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LogicProverTest {
  @Test
  public void testInnerJoinElimination0() {
    final Substitution rule =
        Substitution.parse(
            "Proj<a0>(Filter<p0 b0>(InnerJoin<k0 k1>(Input<t0>,Input<t1>)))|"
                + "Proj<a1>(Filter<p1 b1>(Input<t2>))|"
                + "TableEq(t0,t2);AttrsEq(a0,a1);AttrsEq(b0,b1);PredicateEq(p0,p1);"
                + "AttrsSub(a0,t0);AttrsSub(b0,t0);AttrsSub(k0,t0);AttrsSub(k1,t1);"
                + "NotNull(t0,k0);NotNull(t1,k1);Unique(t1,k1);Reference(t0,k0,t1,k1)");
    final UExprTranslationResult uExprs = UExprSupport.translateToUExpr(rule);
    final int result = LogicSupport.proveEq(uExprs);
    assertEquals(LogicSupport.EQ, result);
  }

  @Test
  public void testInnerJoinElimination1() {
    final Substitution rule =
        Substitution.parse(
            "Proj*<a0>(Filter<p0 b0>(InnerJoin<k0 k1>(Input<t0>,Input<t1>)))|"
                + "Proj*<a1>(Filter<p1 b1>(Input<t2>))|"
                + "TableEq(t0,t2);AttrsEq(a0,a1);AttrsEq(b0,b1);PredicateEq(p0,p1);"
                + "AttrsSub(a0,t0);AttrsSub(b0,t0);AttrsSub(k0,t0);AttrsSub(k1,t1);"
                + "NotNull(t0,k0);NotNull(t1,k1);Reference(t0,k0,t1,k1)");
    final UExprTranslationResult uExprs = UExprSupport.translateToUExpr(rule);
    final int result = LogicSupport.proveEq(uExprs);
    assertEquals(LogicSupport.EQ, result);
  }

  @Test
  public void testIN2InnerJoin0() {
    final Substitution rule =
        Substitution.parse(
            "Proj<a0>(InSubFilter<k0>(Input<t0>,Proj<k1>(Input<t1>)))|"
                + "Proj<a1>(InnerJoin<k2 k3>(Input<t2>,Input<t3>))|"
                + "TableEq(t0,t2);TableEq(t1,t3);AttrsEq(a0,a1);AttrsEq(k0,k2);AttrsEq(k1,k3);"
                + "AttrsSub(a0,t0);AttrsSub(k0,t0);AttrsSub(k1,t1);"
                + "Unique(t1,k1)");
    final UExprTranslationResult uExprs = UExprSupport.translateToUExpr(rule);
    final int result = LogicSupport.proveEq(uExprs);
    assertEquals(LogicSupport.EQ, result);
  }

  @Test
  public void testIN2InnerJoin1() {
    final Substitution rule =
        Substitution.parse(
            "Proj*<a0>(InSubFilter<k0>(Input<t0>,Proj<k1>(Input<t1>)))|"
                + "Proj*<a1>(InnerJoin<k2 k3>(Input<t2>,Input<t3>))|"
                + "TableEq(t0,t2);TableEq(t1,t3);AttrsEq(a0,a1);AttrsEq(k0,k2);AttrsEq(k1,k3);"
                + "AttrsSub(a0,t0);AttrsSub(k0,t0);AttrsSub(k1,t1)");
    final UExprTranslationResult uExprs = UExprSupport.translateToUExpr(rule);
    final int result = LogicSupport.proveEq(uExprs);
    assertEquals(LogicSupport.EQ, result);
  }

  @Test
  public void testPlainFilterCollapsing() {
    final Substitution rule =
        Substitution.parse(
            "Filter<p0 a0>(Filter<p1 a1>(Input<t0>))|"
                + "Filter<p2 a2>(Input<t1>)|"
                + "TableEq(t0,t1);AttrsEq(a0,a1);AttrsEq(a0,a2);PredicateEq(p0,p1);PredicateEq(p0,p2);PredicateEq(p1,p2);"
                + "AttrsSub(a1,t0);AttrsSub(a0,t0)");
    final UExprTranslationResult uExprs = UExprSupport.translateToUExpr(rule);
    final int result = LogicSupport.proveEq(uExprs);
    assertEquals(LogicSupport.EQ, result);
  }

  @Test
  void testINSubFilterCollapsing() {
    final Substitution rule =
        Substitution.parse(
            "InSubFilter<a0>(InSubFilter<a1>(Input<t0>,Input<t1>),Input<t2>)|"
                + "InSubFilter<a2>(Input<t3>,Input<t4>)|"
                + "TableEq(t0,t3);TableEq(t1,t2);TableEq(t1,t4);"
                + "AttrsEq(a0,a1);AttrsEq(a0,a2);"
                + "AttrsSub(a1,t0);AttrsSub(a0,t0);AttrsSub(a2,t3)");
    final UExprTranslationResult uExprs = UExprSupport.translateToUExpr(rule);
    final int result = LogicSupport.proveEq(uExprs);
    assertEquals(LogicSupport.EQ, result);
  }

  @Test
  void testProjCollapsing0() {
    final Substitution rule =
        Substitution.parse(
            "Proj<a0>(Proj<a1>(Input<t0>))|"
                + "Proj<a2>(Input<t1>)|"
                + "TableEq(t0,t1);AttrsEq(a0,a2);AttrsEq(a0,a1);"
                + "AttrsSub(a0,a1);AttrsSub(a1,t0)");
    final UExprTranslationResult uExprs = UExprSupport.translateToUExpr(rule);
    final int result = LogicSupport.proveEq(uExprs);
    assertEquals(LogicSupport.EQ, result);
  }

  @Test
  void testInSubFilterElimination() {
    final Substitution rule =
        Substitution.parse(
            "InSubFilter<a0>(Input<t0>,Proj<a1>(Input<t1>))|"
                + "Input<t2>|"
                + "TableEq(t0,t1);TableEq(t0,t2);AttrsEq(a0,a1);"
                + "AttrsSub(a1,t1);AttrsSub(a0,t0);NotNull(t1,a1);NotNull(t0,a0)");
    final UExprTranslationResult uExprs = UExprSupport.translateToUExpr(rule);
    final int result = LogicSupport.proveEq(uExprs);
    assertEquals(LogicSupport.EQ, result);
  }

  @Test
  void testRemoveDeduplication() {
    final Substitution rule =
        Substitution.parse(
            "Proj*<a0>(Input<t0>)|"
                + "Proj<a1>(Input<t1>)|"
                + "TableEq(t0,t1);AttrsEq(a0,a1);AttrsSub(a0,t0);Unique(t0,a0)");
    final UExprTranslationResult uExprs = UExprSupport.translateToUExpr(rule);
    final int result = LogicSupport.proveEq(uExprs);
    assertEquals(LogicSupport.EQ, result);
  }

  @Test
  void testFlattenJoinSubquery() {
    final Substitution rule =
        Substitution.parse(
            "Proj<a0>(InnerJoin<k0 k1>(Input<t0>,Proj<a1>(Filter<p0 b0>(Input<t1>))))|"
                + "Proj<a2>(Filter<p1 b1>(InnerJoin<k2 k3>(Input<t2>,Input<t3>)))|"
                + "TableEq(t0,t2);TableEq(t1,t3);"
                + "AttrsEq(a0,a2);AttrsEq(k0,k2);AttrsEq(k1,k3);AttrsEq(b0,b1);"
                + "PredicateEq(p0,p1);"
                + "AttrsSub(a0,t0);AttrsSub(k0,t0);AttrsSub(k1,a1);AttrsSub(b0,t1);AttrsSub(a1,t1)");
    final UExprTranslationResult uExprs = UExprSupport.translateToUExpr(rule);
    final int result = LogicSupport.proveEq(uExprs);
    assertEquals(LogicSupport.EQ, result);
  }

  @Test
  void testSubstituteAttr0() {
    final Substitution rule =
        Substitution.parse(
            "Filter<p0 a0>(InnerJoin<a1 a2>(Input<t0>,Input<t1>))|"
                + "Filter<p1 a3>(InnerJoin<a4 a5>(Input<t2>,Input<t3>))|"
                + "TableEq(t0,t2);TableEq(t1,t3);"
                + "AttrsEq(a0,a1);AttrsEq(a0,a4);AttrsEq(a2,a3);AttrsEq(a2,a5);"
                + "PredicateEq(p0,p1);"
                + "AttrsSub(a1,t0);AttrsSub(a2,t1);AttrsSub(a0,t0)");
    final UExprTranslationResult uExprs = UExprSupport.translateToUExpr(rule);
    final int result = LogicSupport.proveEq(uExprs);
    assertEquals(LogicSupport.EQ, result);
  }

  @Test
  void testLeftJoinElimination0() {
    final Substitution rule =
        Substitution.parse(
            "Proj<a0>(Filter<p0 a1>(LeftJoin<a2 a3>(Input<t0>,Input<t1>)))|"
                + "Proj<a4>(Filter<p1 a5>(Input<t2>))|"
                + "TableEq(t0,t2);AttrsEq(a0,a4);AttrsEq(a1,a5);PredicateEq(p0,p1);"
                + "AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a1,t0);AttrsSub(a0,t0);"
                + "Unique(t1,a3)");
    final UExprTranslationResult uExprs = UExprSupport.translateToUExpr(rule);
    final int result = LogicSupport.proveEq(uExprs);
    assertEquals(LogicSupport.EQ, result);
  }

  @Test
  void testLeftJoinElimination1() {
    final Substitution rule =
        Substitution.parse(
            "Proj*<a0>(Filter<p0 a1>(LeftJoin<a2 a3>(Input<t0>,Input<t1>)))|"
                + "Proj*<a4>(Filter<p1 a5>(Input<t2>))|"
                + "TableEq(t0,t2);AttrsEq(a0,a4);AttrsEq(a1,a5);PredicateEq(p0,p1);"
                + "AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a1,t0);AttrsSub(a0,t0)");
    final UExprTranslationResult uExprs = UExprSupport.translateToUExpr(rule);
    final int result = LogicSupport.proveEq(uExprs);
    assertEquals(LogicSupport.EQ, result);
  }

  @Test
  void testLeftJoinToInnerJoin() {
    final Substitution rule =
        Substitution.parse(
            "LeftJoin<k0 k1>(Input<t0>,Input<t1>)|"
                + "InnerJoin<k2 k3>(Input<t2>,Input<t3>)|"
                + "TableEq(t0,t2);TableEq(t1,t3);AttrsEq(k0,k2);AttrsEq(k1,k3);"
                + "AttrsSub(k0,t0);AttrsSub(k1,t1);AttrsSub(k2,t2);AttrsSub(k3,t3);"
                + "NotNull(t0,k0);Reference(t0,k0,t1,k1)");
    final UExprTranslationResult uExprs = UExprSupport.translateToUExpr(rule);
    final int result = LogicSupport.proveEq(uExprs);
    assertEquals(LogicSupport.EQ, result);
  }
}
