package wtune.lab;

import org.junit.jupiter.api.Test;
import wtune.superopt.substitution.Substitution;
import wtune.superopt.uexpr.UExprSupport;
import wtune.superopt.uexpr.UExprTranslationResult;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UExprTest {

    @Test
    public void uExprTest1() {
        final Substitution rule =
                Substitution.parse(
                        "Filter<p0 a0>(Proj<a1 s1>(Input<t0>))|"
                                + "Proj<a2 s2>(Filter<p1 a3>(Input<t1>)|"
                                + "TableEq(t0,t1);AttrsEq(a3,a0);AttrsEq(a2,a1);PredicateEq(p1,p0);"
                                + "AttrsSub(a0,s1);AttrsSub(a1,t0);SchemaEq(s2,s1)");
        final UExprTranslationResult result = UExprSupport.translateToUExpr(rule);
        System.out.println("result se:" + result.sourceExpr());
        System.out.println("result te:" + result.targetExpr());
        System.out.println("result so:" + result.sourceOutVar());
        System.out.println("result to:" + result.targetOutVar());
        assertEquals("∑{x0}([p0(a0(x1))] * [x1 = a1(x0)] * t0(x0))",result.sourceExpr().toString());
        assertEquals("∑{x0}([x1 = a1(x0)] * t0(x0) * [p0(a0(x0))])", result.targetExpr().toString());
        assertEquals("x1", result.sourceOutVar().toString());
        assertEquals("x1", result.targetOutVar().toString());
    }

    @Test
    public void uExprTest2() {
        final Substitution rule =
                Substitution.parse(
                        "Proj*<a2 s0>(InnerJoin<a0 a1>(Input<t0>,Input<t1>))|" +
                                "Proj*<a3 s1>(Input<t2>)|" +
                                "AttrsEq(a1,a2);AttrsSub(a0,t0);AttrsSub(a1,t1);AttrsSub(a2,t1);" +
                                "NotNull(t0,a0);Reference(t0,a0,t1,a1);TableEq(t2,t0);AttrsEq(a3,a0);SchemaEq(s1,s0)");
        final UExprTranslationResult result = UExprSupport.translateToUExpr(rule);
        System.out.println("result se:" + result.sourceExpr());
        System.out.println("result te:" + result.targetExpr());
        System.out.println("result so:" + result.sourceOutVar());
        System.out.println("result to:" + result.targetOutVar());
        assertEquals("||∑{x0,x1}([x2 = a1(x1)] * t0(x0) * [a0(x0) = a1(x1)] * not([IsNull(a1(x1))]) * t1(x1))||",result.sourceExpr().toString());
        assertEquals("||∑{x0}([x2 = a0(x0)] * t0(x0))||", result.targetExpr().toString());
        assertEquals("x2", result.sourceOutVar().toString());
        assertEquals("x2", result.targetOutVar().toString());
    }

    @Test
    public void uExprTest3() {
        final Substitution rule =
                Substitution.parse(
                        "Filter<p0 b0>(InnerJoin<k0 k1>(Input<t0>,Input<t1>))|" +
                                "InnerJoin<k2 k3>(Input<t2>,Filter<p1 b1>(Input<t3>))|" +
                                "AttrsSub(k0,t0);AttrsSub(k1,t1);AttrsSub(b0,t1);TableEq(t2,t0);" +
                                "TableEq(t3,t1);AttrsEq(b1,b0);AttrsEq(k2,k0);AttrsEq(k3,k1);PredicateEq(p1,p0)");

        final UExprTranslationResult result = UExprSupport.translateToUExpr(rule);
        System.out.println("result se:" + result.sourceExpr());
        System.out.println("result te:" + result.targetExpr());
        System.out.println("result so:" + result.sourceOutVar());
        System.out.println("result to:" + result.targetOutVar());
        assertEquals(
                "t0(x0) * [k0(x0) = k1(x1)] * not([IsNull(k1(x1))]) * t1(x1) * [p0(b0(x1))]",
                result.sourceExpr().toString());
        assertEquals("t0(x0) * [k0(x0) = k1(x1)] * not([IsNull(k1(x1))]) * t1(x1) * [p0(b0(x1))]", result.targetExpr().toString());
        assertEquals("concat(x0,x1)", result.sourceOutVar().toString());
        assertEquals("concat(x0,x1)", result.targetOutVar().toString());
    }

}
