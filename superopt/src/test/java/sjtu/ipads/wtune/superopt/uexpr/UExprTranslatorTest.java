package sjtu.ipads.wtune.superopt.uexpr;

import com.microsoft.z3.Context;
import com.microsoft.z3.UninterpretedSort;
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
                + "AttrsSub(a0,t0);AttrsSub(a2,t1);AttrsSub(a1,t1)");
    final UExprTranslationResult result = UExprSupport.translateToUExpr(rule);
    assertEquals(3, result.symToTable.size());
    assertEquals(3, result.symToAttrs.size());
    assertEquals(8, result.varSchemas.size());
    assertEquals(4, result.symSchemas.size());
    System.out.println(result.srcExpr);
    System.out.println(result.tgtExpr);
  }

  @Test
  public void testLeftJoin1() {
    final Substitution rule =
        Substitution.parse(
            "Proj*<a1>(LeftJoin<a2 a3>(Input<t1>,Proj*<a4>(Input<t2>))|"
                + "Proj*<a0>(Input<t0>)|"
                + "TableEq(t0,t1);AttrsEq(a0,a1);AttrsEq(a1,a2);"
                + "AttrsSub(a0,t0);AttrsSub(a2,t1);AttrsSub(a1,t1);AttrsSub(a3,a4);AttrsSub(a4,t2)");
    final UExprTranslationResult result = UExprSupport.translateToUExpr(rule);
    assertEquals(3, result.symToTable.size());
    assertEquals(4, result.symToAttrs.size());
    assertEquals(10, result.varSchemas.size());
    assertEquals(5, result.symSchemas.size());
    System.out.println(result.srcExpr);
    System.out.println(result.tgtExpr);
  }
}
