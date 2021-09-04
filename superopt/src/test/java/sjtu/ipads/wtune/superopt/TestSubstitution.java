package sjtu.ipads.wtune.superopt;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.superopt.optimizer.OptimizerSupport;
import sjtu.ipads.wtune.superopt.substitution.SubstitutionBank;

import java.util.Set;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static sjtu.ipads.wtune.sqlparser.plan.PlanSupport.translateAsAst;
import static sjtu.ipads.wtune.superopt.TestHelper.mkPlan;

@Tag("optimizer")
@Tag("fast")
public class TestSubstitution {
  @Test
  void test0() {
    final String schemaSQL =
        "create table b (j int); create table a (i int not null references b(j));";
    final String sql = "select DISTINCT b.j from a inner join b on a.i = b.j where b.j = 1";
    final String substitution =
        "Proj*<a0>(Filter<p0 a1>(InnerJoin<a2 a3>(Input<t0>,Input<t1>)))"
            + "|Proj*<a4>(Filter<p1 a5>(Input<t2>))"
            + "|TableEq(t0,t2);"
            + "AttrsEq(a0,a1);AttrsEq(a0,a3);AttrsEq(a1,a3);AttrsEq(a2,a4);AttrsEq(a2,a5);AttrsEq(a4,a5);"
            + "PredicateEq(p0,p1);"
            + "AttrsSub(a0,t1);AttrsSub(a1,t1);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a4,t2);AttrsSub(a5,t2);"
            + "Reference(t0,a2,t1,a3)";

    final SubstitutionBank bank = SubstitutionBank.parse(singletonList(substitution));
    final PlanNode plan = mkPlan(sql, schemaSQL);
    final Set<PlanNode> optimized = OptimizerSupport.optimize(bank, plan);

    assertEquals(1, optimized.size());
    assertEquals(
        "SELECT DISTINCT * FROM `a` AS `a` WHERE `a`.`i` = 1",
        translateAsAst(Iterables.get(optimized, 0)).toString());
  }

  @Test
  void test1() {
    final String schemaSQL =
        "create table b (j int); create table a (i int not null references b(j));";
    final String sql =
        "select DISTINCT a.i from a inner join b on a.i = b.j where a.i = 1 and a.i < 2";
    final String substitution =
        "Proj*<a0>(Filter<p0 a1>(InnerJoin<a2 a3>(Input<t0>,Input<t1>)))"
            + "|Proj*<a4>(Filter<p1 a5>(Input<t2>))"
            + "|TableEq(t0,t2);"
            + "AttrsEq(a0,a2);AttrsEq(a0,a4);AttrsEq(a1,a5);"
            + "PredicateEq(p0,p1);"
            + "AttrsSub(a0,t0);AttrsSub(a1,t0);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a4,t2);AttrsSub(a5,t2);"
            + "Reference(t0,a2,t1,a3)";

    final SubstitutionBank bank = SubstitutionBank.parse(singletonList(substitution));
    final PlanNode plan = mkPlan(sql, schemaSQL);
    final Set<PlanNode> optimized = OptimizerSupport.optimize(bank, plan);

    assertEquals(1, optimized.size());
    assertEquals(
        "SELECT DISTINCT * FROM `a` AS `a` WHERE `a`.`i` < 2 AND `a`.`i` = 1",
        translateAsAst(Iterables.get(optimized, 0)).toString());
  }

  @Test
  void test2() {
    final String schemaSQL =
        "create table a (i int); create table b (j int not null references a(i)); ";
    final String sql = "select DISTINCT b.* from a join b on a.i = b.j";
    final String substitution =
        "Proj*<a0>(InnerJoin<a1 a2>(Input<t0>,Input<t1>))"
            + "|Proj*<a3>(Input<t2>)"
            + "|TableEq(t0,t2);AttrsEq(a0,a3);"
            + "AttrsSub(a0,t0);AttrsSub(a1,t0);AttrsSub(a2,t1);AttrsSub(a3,t2);"
            + "Reference(t0,a1,t1,a2)";

    final SubstitutionBank bank = SubstitutionBank.parse(singletonList(substitution));
    final PlanNode plan = mkPlan(sql, schemaSQL);
    final Set<PlanNode> optimized = OptimizerSupport.optimize(bank, plan);

    assertEquals(1, optimized.size());
    assertEquals(
        "SELECT DISTINCT * FROM `b` AS `b`", translateAsAst(Iterables.get(optimized, 0)).toString());
  }

  @Test
  void test3() {
    final String schemaSQL =
        "create table a (i int); "
            + "create table b (j int not null references a(i)); "
            + "create table c (k int)";
    final String sql = "select DISTINCT b.* from a join b on a.i = b.j join c on b.j = c.k";
    final String substitution =
        "Proj*<a0>(InnerJoin<a1 a2>(Input<t0>,Input<t1>))"
            + "|Proj*<a3>(Input<t2>)"
            + "|TableEq(t0,t2);AttrsEq(a0,a3);"
            + "AttrsSub(a0,t0);AttrsSub(a1,t0);AttrsSub(a2,t1);AttrsSub(a3,t2);"
            + "Reference(t0,a1,t1,a2)";

    final SubstitutionBank bank = SubstitutionBank.parse(singletonList(substitution));
    final PlanNode plan = mkPlan(sql, schemaSQL);
    final Set<PlanNode> optimized = OptimizerSupport.optimize(bank, plan);

    assertEquals(1, optimized.size());
    assertEquals(
        "SELECT DISTINCT `b`.`j` AS `j` FROM `b` AS `b` INNER JOIN `c` AS `c` ON `b`.`j` = `c`.`k`",
        translateAsAst(Iterables.get(optimized, 0)).toString());
  }
}
