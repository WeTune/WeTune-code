package sjtu.ipads.wtune.superopt;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.superopt.fragment.Fragment.wrap;
import static sjtu.ipads.wtune.superopt.fragment.Operator.innerJoin;
import static sjtu.ipads.wtune.superopt.fragment.Operator.leftJoin;
import static sjtu.ipads.wtune.superopt.fragment.Operator.plainFilter;
import static sjtu.ipads.wtune.superopt.fragment.Operator.proj;
import static sjtu.ipads.wtune.superopt.fragment.Operator.subqueryFilter;

import java.util.Collection;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Numbering;
import sjtu.ipads.wtune.superopt.internal.Prover;
import sjtu.ipads.wtune.superopt.optimizer.Substitution;

public class TestProof {
  private static String makeSubString(
      Fragment g0, Fragment g1, Numbering numbering, String constraintStr) {
    return g0.toString(numbering) + "|" + g1.toString(numbering) + "|" + constraintStr;
  }

  private static final class TestHelper {
    private Fragment g0, g1;
    private Collection<Substitution> results;
    private Collection<String> strs;
    private Numbering numbering;

    private void solve() {
      if (results != null) return;
      if (g0 == null || g1 == null) throw new IllegalStateException();
      results = Prover.prove(g0, g1, -1);
      strs = results == null ? null : listMap(Object::toString, results);
      numbering = Numbering.make();
      numbering.number(g0, g1);
    }

    private void check(String target) {
      solve();
      if (strs != null) assertTrue(strs.contains(makeSubString(g0, g1, numbering, target)));
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
        "TableEq(t0,t2);PickEq(c0,c3);Reference(t0,c1,t1,c2);PickFrom(c0,[t0]);PickFrom(c1,[t0]);PickFrom(c2,[t1]);PickFrom(c3,[t2])");
    test.check(
        "TableEq(t0,t2);PickEq(c0,c2);PickEq(c1,c3);Reference(t0,c1,t1,c2);PickFrom(c0,[t1]);PickFrom(c1,[t0]);PickFrom(c2,[t1]);PickFrom(c3,[t2])");
  }

  @Test
  void testRemoveLeftJoin0() {
    final TestHelper test = new TestHelper();
    test.g0 = wrap(proj(leftJoin(null, null)));
    test.g1 = wrap(proj(null));
    test.print();
    test.check("TableEq(t0,t2);PickEq(c0,c3);PickFrom(c0,[t0]);PickFrom(c3,[t2])");
  }

  @Test
  void testRemoveLeftJoin1() {
    final TestHelper test = new TestHelper();
    test.g0 = wrap(proj(plainFilter(leftJoin(null, null))));
    test.g1 = wrap(proj(plainFilter(null)));
    test.print();
    test.check(
        "TableEq(t0,t2);PickEq(c0,c4);PickEq(c1,c5);PredicateEq(p0,p1);PickFrom(c0,[t0]);PickFrom(c1,[t0]);PickFrom(c4,[t2]);PickFrom(c5,[t2])");
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
        "TableEq(t0,t2);TableEq(t1,t3);PickEq(c0,c2);PickEq(c1,c3);Reference(t2,c2,t3,c3);PickFrom(c0,[t0]);PickFrom(c1,[t1]);PickFrom(c2,[t2]);PickFrom(c3,[t3])");
  }

  @Test
  void testChangeFilterColumn() {
    final TestHelper test = new TestHelper();
    test.g0 = wrap(plainFilter(innerJoin(null, null)));
    test.g1 = wrap(plainFilter(innerJoin(null, null)));
    test.print();
    test.check(
        "TableEq(t0,t2);TableEq(t1,t3);PickEq(c0,c1);PickEq(c0,c4);PickEq(c1,c4);PickEq(c2,c3);PickEq(c2,c5);PickEq(c3,c5);PredicateEq(p0,p1);PickFrom(c0,[t0]);PickFrom(c1,[t0]);PickFrom(c2,[t1]);PickFrom(c3,[t3]);PickFrom(c4,[t2]);PickFrom(c5,[t3])");
  }
}
