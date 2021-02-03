package sjtu.ipads.wtune.superopt;

import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.superopt.plan.Plan;
import sjtu.ipads.wtune.superopt.substitution.Substitution;
import sjtu.ipads.wtune.superopt.internal.Prove;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.superopt.plan.Plan.wrap;
import static sjtu.ipads.wtune.superopt.plan.PlanNode.*;

public class TestProof {
  private static String makeSubString(Plan g0, Plan g1, String constraintStr) {
    return g0.toInformativeString() + "|" + g1.toInformativeString() + "|" + constraintStr;
  }

  private static final class TestHelper {
    private Plan g0, g1;
    private Collection<Substitution> results;
    private Collection<String> strs;

    private void solve() {
      if (results != null) return;
      if (g0 == null || g1 == null) throw new IllegalStateException();
      results = Prove.proveEq(g0, g1, -1);
      strs = results == null ? null : listMap(Object::toString, results);
    }

    private void check(String target) {
      solve();
      if (strs != null) assertTrue(strs.contains(makeSubString(g0, g1, target)));
    }

    private void print() {
      solve();
      if (strs != null) strs.forEach(System.out::println);
    }
  }

  @Test
  void testRemoveInnerJoin() {
    final TestHelper test = new TestHelper();
    test.g0 = wrap(proj(innerJoin(null, null)));
    test.g1 = wrap(proj(null));
    test.print();

    test.check(
        "TableEq(t0,t2);PickEq(c0,c3);"
            + "PickFrom(c0,[t0]);PickFrom(c1,[t0]);PickFrom(c2,[t1]);PickFrom(c3,[t2]);"
            + "Reference(t0,c1,t1,c2)");

    test.check(
        "TableEq(t0,t2);PickEq(c0,c2);PickEq(c1,c3);"
            + "PickFrom(c0,[t1]);PickFrom(c1,[t0]);PickFrom(c2,[t1]);PickFrom(c3,[t2]);"
            + "Reference(t0,c1,t1,c2)");
  }

  @Test
  void testRemoveLeftJoin() {
    final TestHelper test = new TestHelper();
    test.g0 = wrap(proj(leftJoin(null, null)));
    test.g1 = wrap(proj(null));
    test.print();
    test.check("TableEq(t0,t2);PickEq(c0,c3);PickFrom(c0,[t0]);PickFrom(c3,[t2])");
  }

  @Test
  void testSubqueryToJoin() {
    final TestHelper test = new TestHelper();
    test.g0 = wrap(proj(innerJoin(null, null)));
    test.g1 = wrap(proj(subqueryFilter(null, proj(null))));
    test.print();
    test.check(
        "TableEq(t0,t2);TableEq(t1,t3);PickEq(c0,c3);PickEq(c1,c4);PickEq(c2,c5);PickFrom(c0,[t0]);PickFrom(c1,[t0]);PickFrom(c2,[t1]);PickFrom(c3,[t2]);PickFrom(c4,[t2]);PickFrom(c5,[t3])");
  }

  @Test
  void testRemoveSubquery() {
    final TestHelper test = new TestHelper();
    test.g0 = wrap(innerJoin(null, plainFilter(null)));
    test.g1 = wrap(plainFilter(innerJoin(null, null)));
    test.print();
    test.check(
        "TableEq(t0,t2);TableEq(t1,t3);PickEq(c0,c4);PickEq(c1,c5);PickEq(c2,c3);PredicateEq(p0,p1);PickFrom(c0,[t0])"
            + ";PickFrom(c1,[t1]);PickFrom(c2,[t1]);PickFrom(c3,[t3]);PickFrom(c4,[t2]);PickFrom(c5,[t3])");
  }

  @Test
  void testLeftToInner() {
    final TestHelper test = new TestHelper();
    test.g0 = wrap(innerJoin(null, null));
    test.g1 = wrap(leftJoin(null, null));
    test.print();
    test.check(
        "TableEq(t0,t2);TableEq(t1,t3);PickEq(c0,c2);PickEq(c1,c3);PickFrom(c0,[t0]);PickFrom(c1,[t1]);PickFrom(c2,[t2]);PickFrom(c3,[t3]);Reference(t2,c2,t3,c3)");
  }
}
