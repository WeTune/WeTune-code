package sjtu.ipads.wtune.superopt.optimizer;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sql.plan.PlanContext;
import sjtu.ipads.wtune.superopt.substitution.Substitution;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sjtu.ipads.wtune.sql.plan.PlanSupport.translateAsAst;
import static sjtu.ipads.wtune.superopt.TestHelper.parsePlan;

@Tag("optimizer")
@Tag("fast")
public class InstantiationTest {
  @Test
  void test0() {
    final PlanContext plan =
        parsePlan(
            "Select c.u From c Inner Join d On c.u = d.p Where d.q = 2 And d.r In (Select c.w From c)");
    final Substitution rule =
        Substitution.parse(
            "Proj<a0 s0>(Filter<p0 b0>(InnerJoin<k0 k1>(Input<t0>,Input<t1>)))|"
                + "Proj<a1 s1>(Filter<p1 b1>(Input<t2>))|"
                + "AttrsEq(a0,k1);"
                + "AttrsSub(a0,t1);AttrsSub(b0,t0);AttrsSub(k0,t0);AttrsSub(k1,t1);"
                + "NotNull(t0,k0);Unique(t1,k1);Reference(t0,k0,t1,k1);"
                + "TableEq(t2,t0);SchemaEq(s1,s0);AttrsEq(a1,a0);AttrsEq(b1,b0);PredicateEq(p1,p0)");

    final Match match = new Match(rule).setSourcePlan(plan).setMatchRootNode(plan.root());
    final List<Match> result = Match.match(match, rule._0().root(), plan.root());

    assertEquals(1, result.size());
    assertTrue(result.get(0).mkModifiedPlan());
    final PlanContext optimized = result.get(0).modifiedPlan();
    assertEquals(
        "SELECT `c`.`u` AS `u` FROM `d` AS `d` WHERE `d`.`q` = 2 AND `d`.`r` IN (SELECT `q0`.`w` AS `w` FROM `c` AS `q0`)",
        translateAsAst(optimized, optimized.root(), false).toString());
  }

  @Test
  void test1() {
    final PlanContext plan = parsePlan("Select sub.i From (Select a.i, a.j From a) As sub");
    final Substitution rule =
        Substitution.parse(
            "Proj<a0 s0>(Proj<a1 s1>(Input<t0>))|"
                + "Proj<a2 s2>(Input<t1>)|"
                + "AttrsSub(a0,s1);AttrsSub(a1,t0);"
                + "TableEq(t1,t0);SchemaEq(s2,s0);AttrsEq(a2,a1)");

    final Match match = new Match(rule).setSourcePlan(plan).setMatchRootNode(plan.root());
    final List<Match> result = Match.match(match, rule._0().root(), plan.root());

    assertEquals(1, result.size());
    assertTrue(result.get(0).mkModifiedPlan());
    final PlanContext optimized = result.get(0).modifiedPlan();
    assertEquals(
        "SELECT `a`.`i` AS `i` FROM `a` AS `a`",
        translateAsAst(optimized, optimized.root(), false).toString());
  }

  @Test
  void test2() {
    final PlanContext plan = parsePlan("Select * From a Where a.i In (Select b.x From b)");
    final Substitution rule =
        Substitution.parse(
            "Proj<a0 s0>(SubqueryFilter<b0>(Input<t0>,Proj<b1 s1>(Input<t1>)))|"
                + "Proj<a2 s2>(InnerJoin<k0 k1>(Input<t2>,Input<t3>))|"
                + "AttrsSub(a0,t0);AttrsSub(b0,t0);AttrsSub(b1,t1);"
                + "TableEq(t2,t0);TableEq(t3,t1);SchemaEq(s2,s0);AttrsEq(a2,a0);AttrsEq(k0,b0);AttrsEq(k1,b1)");

    final Match match = new Match(rule).setSourcePlan(plan).setMatchRootNode(plan.root());
    final List<Match> result = Match.match(match, rule._0().root(), plan.root());

    assertEquals(1, result.size());
    assertTrue(result.get(0).mkModifiedPlan());
    final PlanContext optimized = result.get(0).modifiedPlan();
    assertEquals(
        "SELECT `a`.`i` AS `i`, `a`.`j` AS `j`, `a`.`k` AS `k` "
            + "FROM `a` AS `a` INNER JOIN `b` AS `b` ON `a`.`i` = `b`.`x`",
        translateAsAst(optimized, optimized.root(), false).toString());
  }
}
