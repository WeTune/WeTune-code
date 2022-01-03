package sjtu.ipads.wtune.superopt.optimizer;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sql.plan.PlanContext;
import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.util.Fingerprint;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static sjtu.ipads.wtune.common.utils.FuncUtils.setMap;
import static sjtu.ipads.wtune.superopt.TestHelper.parsePlan;

@Tag("optimizer")
@Tag("fast")
public class FingerprintTest {
  @Test
  void testOp() {
    final Fragment fragment =
        Fragment.parse("Proj(Filter(InSubFilter(InnerJoin(Input,Input),Input)))", null);
    assertEquals("pfsj", Fingerprint.mk(fragment).toString());
  }

  @Test
  void testFilter() {
    final PlanContext plan =
        parsePlan(
            "Select a.* From a "
                + "Where a.i = 0 "
                + "And a.i In (Select a.* From a Where a.j > 10) "
                + "And a.i < 1 "
                + "And a.i In (Select a.* From a Where a.j > 20) "
                + "And a.i  < 2");

    final Set<Fingerprint> fingerprints = Fingerprint.mk(plan, plan.root());
    assertEquals(9, fingerprints.size());
    assertEquals(
        Set.of("p", "pf", "ps", "pff", "pfs", "pss", "pfff", "pffs", "pfss"),
        setMap(fingerprints, Fingerprint::toString));
  }

  @Test
  void testJoin() {
    final PlanContext plan =
        parsePlan(
            "Select * From a "
                + "Join b On a.i = b.x "
                + "Join c On a.i = c.u "
                + "Join d On a.i = d.p "
                + "Left Join a As a1 On a.i = a1.i "
                + "Left Join b As b1 On a.i = b1.x");

    final Set<Fingerprint> fingerprints = Fingerprint.mk(plan, plan.root());
    assertEquals(14, fingerprints.size());
    assertEquals(
        Set.of(
            "p", "pl", "pj", "pjj", "pjl", "plj", "pll", "pjjj", "pjjl", "pjlj", "pljj", "pjll",
            "pljl", "pllj"),
        setMap(fingerprints, Fingerprint::toString));
  }
}
