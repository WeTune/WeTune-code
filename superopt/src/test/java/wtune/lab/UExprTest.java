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
                        "InSubFilter<k0>(Input<t0>,Proj<k1 s1>(Input<t1>))|"
                                + "Input<t2>|"
                                + "TableEq(t2,t0);"
                                + "AttrsSub(k0,t0);AttrsSub(k1,t1)");

        final UExprTranslationResult result = UExprSupport.translateToUExpr(rule);
        System.out.println("result se:" + result.sourceExpr());
        System.out.println("result te:" + result.targetExpr());
        System.out.println("result so:" + result.sourceOutVar());
        System.out.println("result to:" + result.targetOutVar());
        assertEquals(
                "t0(x0) * ||∑{x1}([k0(x0) = k1(x1)] * not([IsNull(k1(x1))]) * t1(x1))||",
                result.sourceExpr().toString());
        assertEquals("t0(x0)", result.targetExpr().toString());
        assertEquals("x0", result.sourceOutVar().toString());
        assertEquals("x0", result.targetOutVar().toString());
    }

    @Test
    public void uExprTest2() {
        final Substitution rule =
                Substitution.parse(
                        "Filter<p0 a0>(Input<t0>)|"
                                + "Filter<p1 a3>(Input<t1>)|"
                                + "TableEq(t0,t1);AttrsEq(a3,a0);PredicateEq(p1,p0);"
                                + "AttrsSub(a0,t0);AttrsSub(a3,t1);");
        final UExprTranslationResult result = UExprSupport.translateToUExpr(rule);
        System.out.println("result se:" + result.sourceExpr());
        System.out.println("result te:" + result.targetExpr());
        System.out.println("result so:" + result.sourceOutVar());
        System.out.println("result to:" + result.targetOutVar());
        assertEquals("t0(x0) * [p0(a0(x0))]",result.sourceExpr().toString());
        assertEquals("t0(x0) * [p0(a0(x0))]", result.targetExpr().toString());
        assertEquals("x0", result.sourceOutVar().toString());
        assertEquals("x0", result.targetOutVar().toString());
    }

    @Test
    public void uExprTest3() {
        final Substitution rule =
                Substitution.parse(
                        "Filter<p0 a0>(Proj<a1 s1>(Input<t0>))|"
                                + "Proj<a2 s2>(Filter<p1 a3>(Input<t1>)|"
                                + "TableEq(t0,t1);AttrsEq(a3,a0);AttrsEq(a2,a1);PredicateEq(p1,p0);"
                                + "AttrsSub(a0,s1);AttrsSub(a1,t0);"
                                + "SchemaEq(s2,s1)");
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
}
