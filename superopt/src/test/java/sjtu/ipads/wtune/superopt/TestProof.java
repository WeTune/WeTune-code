package sjtu.ipads.wtune.superopt;

import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.superopt.core.Graph;
import sjtu.ipads.wtune.superopt.core.Substitution;
import sjtu.ipads.wtune.superopt.internal.Prove;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.superopt.operator.Operator.*;

public class TestProof {
  private static String makeSubString(Graph g0, Graph g1, String constraintStr) {
    return g0.toInformativeString() + "|" + g1.toInformativeString() + "|" + constraintStr;
  }

  @Test
  void testRemoveInnerJoin() {
    final Graph g0 = Graph.wrap(proj(innerJoin(null, null)));
    final Graph g1 = Graph.wrap(proj(null));

    final Collection<Substitution> results = Prove.prove(g0, g1, -1);
    final Collection<String> strs = listMap(Object::toString, results);

    final String target0 =
        makeSubString(
            g0,
            g1,
            "TableEq(t0,t2);PickEq(c0,c3);"
                + "PickFrom(c0,[t0]);PickFrom(c1,[t0]);PickFrom(c2,[t1]);PickFrom(c3,[t2]);"
                + "Reference(t0,c1,t1,c2)");
    final String target1 =
        makeSubString(
            g0,
            g1,
            "TableEq(t0,t2);PickEq(c0,c2);PickEq(c1,c3);"
                + "PickFrom(c0,[t1]);PickFrom(c1,[t0]);PickFrom(c2,[t1]);PickFrom(c3,[t2]);"
                + "Reference(t0,c1,t1,c2)");
    assertTrue(strs.contains(target0));
    assertTrue(strs.contains(target1));
  }

  @Test
  void testRemoveLeftJoin() {
    final Graph g0 = Graph.wrap(proj(leftJoin(null, null)));
    final Graph g1 = Graph.wrap(proj(null));
    final Collection<Substitution> results = Prove.prove(g0, g1, -1);
    final Collection<String> strs = listMap(Object::toString, results);
    final String target =
        makeSubString(g0, g1, "TableEq(t0,t2);PickEq(c0,c3);PickFrom(c0,[t0]);PickFrom(c3,[t2])");
    assertTrue(strs.contains(target));
  }

  @Test
  void testSubqueryToJoin() {
    final Graph g0 = Graph.wrap(proj(innerJoin(null, null)));
    final Graph g1 = Graph.wrap(proj(subqueryFilter(null, proj(null))));
    final Collection<Substitution> results = Prove.prove(g0, g1, -1);
    final Collection<String> strs = listMap(Object::toString, results);

    final String target =
        makeSubString(
            g0,
            g1,
            "TableEq(t0,t2);TableEq(t1,t3);PickEq(c0,c3);PickEq(c1,c4);PickEq(c2,c5);PickFrom(c0,[t0]);PickFrom(c1,[t0]);PickFrom(c2,[t1]);PickFrom(c3,[t2]);PickFrom(c4,[t2]);PickFrom(c5,[t3])");
    assertTrue(strs.contains(target));
  }
}
