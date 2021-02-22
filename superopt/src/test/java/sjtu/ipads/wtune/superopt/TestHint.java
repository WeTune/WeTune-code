package sjtu.ipads.wtune.superopt;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sqlparser.ASTParser;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan.ToASTTranslator;
import sjtu.ipads.wtune.sqlparser.plan.ToPlanTranslator;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.superopt.fragment.Operator;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Interpretations;
import sjtu.ipads.wtune.superopt.optimization.Hint;
import sjtu.ipads.wtune.superopt.optimization.Substitution;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sjtu.ipads.wtune.common.utils.FuncUtils.func;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.ast.ASTNode.MYSQL;

public class TestHint {
  @Test
  void test0() {
    final String schema =
        "create table a (i int); "
            + "create table b (j int references a(i)); "
            + "create table c (k int)";
    final String sql = "select a.* from a join b on a.i = b.j";
    final String substitution =
        "Proj<c0>(InnerJoin<c1 c2>(Input<t0>,Input<t1>))"
            + "|Proj<c3>(Input<t2>)"
            + "|TableEq(t0,t2);PickEq(c0,c3);"
            + "PickFrom(c0,[t0]);PickFrom(c1,[t0]);PickFrom(c2,[t1]);PickFrom(c3,[t2]);"
            + "Reference(t0,c1,t1,c2)";
    final ASTNode ast = ASTParser.mysql().parse(sql);
    ast.context().setSchema(Schema.parse(MYSQL, schema));

    final PlanNode joinNode = ToPlanTranslator.translate(ast).predecessors()[0];
    final Substitution sub = Substitution.rebuild(substitution);
    final Operator joinOp = sub.g0().head().predecessors()[0];
    final Interpretations inter = Interpretations.build(sub.constraints());

    final Iterable<PlanNode> mutated = Hint.apply(joinNode, joinOp, inter);
    assertEquals(1, Iterables.size(mutated));
    assertEquals(
        "SELECT * FROM `b` AS `b` INNER JOIN `a` AS `a` ON `b`.`j` = `a`.`i`",
        ToASTTranslator.translate(Iterables.getOnlyElement(mutated)).toString());
  }

  @Test
  void test1() {
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

    final PlanNode joinNode = ToPlanTranslator.translate(ast).predecessors()[0];
    final Substitution sub = Substitution.rebuild(substitution);
    final Operator joinOp = sub.g0().head().predecessors()[0];
    final Interpretations inter = Interpretations.build(sub.constraints());

    final Iterable<PlanNode> mutated = Hint.apply(joinNode, joinOp, inter);
    assertEquals(1, Iterables.size(mutated));
    assertEquals(
        "SELECT * FROM `b` AS `b` INNER JOIN `c` AS `c` ON `b`.`j` = `c`.`k` INNER JOIN `a` AS `a` ON `b`.`j` = `a`.`i`",
        ToASTTranslator.translate(Iterables.getOnlyElement(mutated)).toString());
  }

  @Test
  void test2() {
    final String schema = "create table a (i int); create table b (j int); create table c (k int)";
    final String sql = "select a.* from a join b on a.i = b.j where a.i = 10 and b.j = 1";
    final String substitution =
        "PlainFilter<p0 c0>(PlainFilter<p1 c1>(InnerJoin<c2 c3>(Input<t0>,Input<t1>)))"
            + "|PlainFilter<p2 c4>(PlainFilter<p3 c5>(InnerJoin<c6 c7>(Input<t2>,Input<t3>)))"
            + "|TableEq(t0,t2);TableEq(t1,t3);"
            + "PickEq(c0,c2);PickEq(c0,c4);PickEq(c0,c5);PickEq(c0,c6);"
            + "PickEq(c1,c3);PickEq(c1,c7);PickEq(c2,c4);PickEq(c2,c5);PickEq(c2,c6);"
            + "PickEq(c3,c7);PickEq(c4,c5);PickEq(c4,c6);PickEq(c5,c6)"
            + "PickFrom(c0,[t0]);PickFrom(c1,[t1]);PickFrom(c2,[t0]);PickFrom(c3,[t1]);"
            + "PickFrom(c4,[t2]);PickFrom(c5,[t2]);PickFrom(c6,[t2]);PickFrom(c7,[t3]);";
    final ASTNode ast = ASTParser.mysql().parse(sql);
    ast.context().setSchema(Schema.parse(MYSQL, schema));

    final PlanNode filterNode = ToPlanTranslator.translate(ast).predecessors()[0];
    final Substitution sub = Substitution.rebuild(substitution);
    final Operator filterOp = sub.g0().head();
    final Interpretations inter = Interpretations.build(sub.constraints());

    final Iterable<PlanNode> mutated = Hint.apply(filterNode, filterOp, inter);
    final List<String> str =
        listMap(func(ToASTTranslator::translate).andThen(ASTNode::toString), mutated);
    assertEquals(2, str.size());
    assertTrue(
        str.contains(
            "SELECT * FROM `a` AS `a` INNER JOIN `b` AS `b` ON `a`.`i` = `b`.`j` WHERE `b`.`j` = 1 AND `a`.`i` = 10"));
    assertTrue(
        str.contains(
            "SELECT * FROM `a` AS `a` INNER JOIN `b` AS `b` ON `a`.`i` = `b`.`j` WHERE `a`.`i` = 10 AND `b`.`j` = 1"));
  }
}
