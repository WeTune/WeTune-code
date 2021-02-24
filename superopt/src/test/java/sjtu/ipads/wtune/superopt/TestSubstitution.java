package sjtu.ipads.wtune.superopt;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sqlparser.ASTParser;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan.ToASTTranslator;
import sjtu.ipads.wtune.sqlparser.plan.ToPlanTranslator;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.superopt.internal.Optimizer;
import sjtu.ipads.wtune.superopt.optimization.Substitution;
import sjtu.ipads.wtune.superopt.optimization.SubstitutionBank;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static sjtu.ipads.wtune.sqlparser.ast.ASTNode.MYSQL;

public class TestSubstitution {
  @Test
  void test0() {
    final String schema = "create table b (j int);" + "create table a (i int references b(j));";
    final String sql = "select b.j from a inner join b on a.i = b.j where b.j = 1";
    final String substitution =
        "Proj<c0>(PlainFilter<p0 c1>(InnerJoin<c2 c3>(Input<t0>,Input<t1>)))"
            + "|Proj<c4>(PlainFilter<p1 c5>(Input<t2>))"
            + "|TableEq(t0,t2);PickEq(c0,c1);PickEq(c0,c3);PickEq(c1,c3);PickEq(c2,c4);PickEq(c2,c5);PickEq(c4,c5);"
            + "PredicateEq(p0,p1);"
            + "PickFrom(c0,[t1]);PickFrom(c1,[t1]);PickFrom(c2,[t0]);PickFrom(c3,[t1]);PickFrom(c4,[t2]);PickFrom(c5,[t2]);"
            + "Reference(t0,c2,t1,c3)";

    final ASTNode ast = ASTParser.mysql().parse(sql);
    ast.context().setSchema(Schema.parse(MYSQL, schema));

    final Substitution sub = Substitution.rebuild(substitution);
    final SubstitutionBank repo = SubstitutionBank.make().add(sub);

    final Optimizer opt = new Optimizer(repo);
    final List<PlanNode> optimized = opt.optimize(ToPlanTranslator.translate(ast));

    assertEquals(1, optimized.size());
    assertEquals(
        "SELECT `a`.`i` AS `i` FROM `a` AS `a` WHERE `a`.`i` = 1",
        ToASTTranslator.translate(Iterables.getOnlyElement(optimized)).toString());
  }

  @Test
  void test1() {
    final String schema = "create table a (i int);";
    final String sql = "select b.i from (select a.i from a) as b";
    final String substitution =
        "Proj<c0>(Proj<c1>(Input<t0>))"
            + "|Proj<c2>(Input<t1>)"
            + "|TableEq(t0,t1);PickEq(c0,c1);PickEq(c0,c2);PickEq(c1,c2);"
            + "PickFrom(c0,[t0]);PickFrom(c1,[t0]);PickFrom(c2,[t1])";

    final ASTNode ast = ASTParser.mysql().parse(sql);
    ast.context().setSchema(Schema.parse(MYSQL, schema));

    final Substitution sub = Substitution.rebuild(substitution);
    final SubstitutionBank repo = SubstitutionBank.make().add(sub);

    final Optimizer opt = new Optimizer(repo);
    final List<PlanNode> optimized = opt.optimize(ToPlanTranslator.translate(ast));

    assertEquals(1, optimized.size());
    assertEquals(
        "SELECT `a`.`i` AS `i` FROM `a` AS `a`",
        ToASTTranslator.translate(Iterables.getOnlyElement(optimized)).toString());
  }

  @Test
  void test2() {
    final String schema =
        "create table a (i int); "
            + "create table b (j int references a(i)); "
            + "create table c (k int)";
    final String sql = "select b.* from a join b on a.i = b.j";
    final String substitution =
        "Proj<c0>(InnerJoin<c1 c2>(Input<t0>,Input<t1>))"
            + "|Proj<c3>(Input<t2>)"
            + "|TableEq(t0,t2);PickEq(c0,c3);"
            + "PickFrom(c0,[t0]);PickFrom(c1,[t0]);PickFrom(c2,[t1]);PickFrom(c3,[t2]);"
            + "Reference(t0,c1,t1,c2)";
    final ASTNode ast = ASTParser.mysql().parse(sql);
    ast.context().setSchema(Schema.parse(MYSQL, schema));

    final PlanNode plan = ToPlanTranslator.translate(ast);
    final SubstitutionBank repo = SubstitutionBank.make().add(Substitution.rebuild(substitution));

    final List<PlanNode> optimized = new Optimizer(repo).optimize(plan);
    assertEquals(1, optimized.size());
    assertEquals(
        "SELECT `b`.`j` AS `j` FROM `b` AS `b`",
        ToASTTranslator.translate(Iterables.getOnlyElement(optimized)).toString());
  }

  @Test
  void test3() {
    final String schema =
        "create table a (i int); "
            + "create table b (j int references a(i)); "
            + "create table c (k int)";
    final String sql = "select b.* from a join b on a.i = b.j join c on b.j = c.k";
    final String substitution =
        "Proj<c0>(InnerJoin<c1 c2>(Input<t0>,Input<t1>))"
            + "|Proj<c3>(Input<t2>)"
            + "|TableEq(t0,t2);PickEq(c0,c3);"
            + "PickFrom(c0,[t0]);PickFrom(c1,[t0]);PickFrom(c2,[t1]);PickFrom(c3,[t2]);"
            + "Reference(t0,c1,t1,c2)";
    final ASTNode ast = ASTParser.mysql().parse(sql);
    ast.context().setSchema(Schema.parse(MYSQL, schema));

    final PlanNode plan = ToPlanTranslator.translate(ast);
    final SubstitutionBank repo = SubstitutionBank.make().add(Substitution.rebuild(substitution));

    final List<PlanNode> optimized = new Optimizer(repo).optimize(plan);
    assertEquals(1, optimized.size());
    assertEquals(
        "SELECT `b`.`j` AS `j` FROM `b` AS `b` INNER JOIN `c` AS `c` ON `b`.`j` = `c`.`k`",
        ToASTTranslator.translate(Iterables.getOnlyElement(optimized)).toString());
  }
}
