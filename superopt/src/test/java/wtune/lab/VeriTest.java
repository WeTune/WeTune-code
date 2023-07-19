package wtune.lab;

import com.microsoft.z3.Global;
import org.junit.jupiter.api.Test;
import wtune.superopt.logic.LogicSupport;
import wtune.superopt.substitution.Substitution;
import wtune.superopt.uexpr.UExprSupport;
import wtune.superopt.uexpr.UExprTranslationResult;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VeriTest {

    @Test
    public void testFOL1() {
        Global.setParameter("timeout", "10000");
        final Substitution rule =
                Substitution.parse(
                        "InSub<k0>(Input<t0>,Proj<k1 s0>(Input<t1>))|" +
                                "Input<t2>|" +
                                "AttrsSub(k0,t0);AttrsSub(k1,t1);NotNull(t0,k0);NotNull(t1,k1);TableEq(t0,t1);AttrsEq(k0,k1);TableEq(t2,t0)");
        final UExprTranslationResult uExprs = UExprSupport.translateToUExpr(rule);
        final int result = LogicSupport.proveEq(uExprs);
        assertEquals(LogicSupport.EQ, result, rule.toString());
    }

    @Test
    public void testFOL2() {
        Global.setParameter("timeout", "10000");
        final Substitution rule =
                Substitution.parse(
                        "Filter<p0 b0>(Proj<a0 s0>(Input<t0>))|"
                                + "Proj<a1 s1>(Filter<p1 b1>(Input<t1>))|"
                                + "AttrsSub(b0,s0);AttrsSub(a0,t0);SchemaEq(s1,s0);TableEq(t1,t0);AttrsEq(b1,b0);AttrsEq(a1,a0);PredicateEq(p1,p0)");
        final UExprTranslationResult uExprs = UExprSupport.translateToUExpr(rule);
        final int result = LogicSupport.proveEq(uExprs);
        assertEquals(LogicSupport.EQ, result, rule.toString());
    }

    @Test
    public void testFOL3() {
        Global.setParameter("timeout", "10000");
        final Substitution rule =
                Substitution.parse(
                        "Proj<a3 s0>(Filter<p0 a2>(InnerJoin<a0 a1>(Input<t0>,Input<t1>)))|" +
                                "Proj<a7 s1>(Filter<p1 a6>(InnerJoin<a4 a5>(Input<t2>,Input<t3>)))|" +
                                "AttrsEq(a1,a3);AttrsSub(a0,t0);AttrsSub(a1,t1);AttrsSub(a2,t0);AttrsSub(a3,t1);TableEq(t2,t0);TableEq(t3,t1);AttrsEq(a4,a0);AttrsEq(a5,a1);AttrsEq(a6,a2);AttrsEq(a7,a0);PredicateEq(p1,p0);SchemaEq(s1,s0)");
        final UExprTranslationResult uExprs = UExprSupport.translateToUExpr(rule);
        final int result = LogicSupport.proveEq(uExprs);
        assertEquals(LogicSupport.EQ, result, rule.toString());

        final Substitution rule2 =
                Substitution.parse(
                        "Proj<a3 s0>(Filter<p0 a2>(InnerJoin<a0 a1>(Input<t0>,Input<t1>)))|" +
                                "Proj<a7 s1>(Filter<p1 a6>(InnerJoin<a4 a5>(Input<t2>,Input<t3>)))|" +
                                "AttrsSub(a0,t0);AttrsSub(a1,t1);AttrsSub(a2,t0);AttrsSub(a3,t1);TableEq(t2,t0);TableEq(t3,t1);AttrsEq(a4,a0);AttrsEq(a5,a1);AttrsEq(a6,a2);AttrsEq(a7,a0);PredicateEq(p1,p0);SchemaEq(s1,s0)");
        final UExprTranslationResult uExprs2 = UExprSupport.translateToUExpr(rule2);
        final int result2 = LogicSupport.proveEq(uExprs2);
        assertEquals(LogicSupport.NEQ, result2, rule.toString());
    }

}
