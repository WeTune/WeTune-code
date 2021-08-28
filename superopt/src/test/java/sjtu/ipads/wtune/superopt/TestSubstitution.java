package sjtu.ipads.wtune.superopt;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sqlparser.ASTParser;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.superopt.optimizer.OptimizerSupport;
import sjtu.ipads.wtune.superopt.substitution.SubstitutionBank;

import java.util.Set;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static sjtu.ipads.wtune.sqlparser.ast.ASTNode.MYSQL;

public class TestSubstitution {
  @Test
  void test0() {
    final String schemaSQL =
        "create table b (j int);" + "create table a (i int not null references b(j));";
    final String sql = "select DISTINCT b.j from a inner join b on a.i = b.j where b.j = 1";
    final String substitution =
        "Proj<c0>(PlainFilter<p0 c1>(InnerJoin<c2 c3>(Input<t0>,Input<t1>)))"
            + "|Proj<c4>(PlainFilter<p1 c5>(Input<t2>))"
            + "|TableEq(t0,t2);PickEq(c0,c1);PickEq(c0,c3);PickEq(c1,c3);PickEq(c2,c4);PickEq(c2,c5);PickEq(c4,c5);"
            + "PredicateEq(p0,p1);"
            + "PickFrom(c0,[t1]);PickFrom(c1,[t1]);PickFrom(c2,[t0]);PickFrom(c3,[t1]);PickFrom(c4,[t2]);PickFrom(c5,[t2]);"
            + "Reference(t0,c2,t1,c3)";

    final SubstitutionBank bank = SubstitutionBank.parse(singletonList(substitution));
    final ASTNode ast = ASTParser.mysql().parse(sql);
    final Schema schema = Schema.parse(MYSQL, schemaSQL);
    ast.context().setSchema(schema);
    final Set<ASTNode> optimized = OptimizerSupport.optimize(bank, schema, ast);

    assertEquals(1, optimized.size());
    assertEquals(
        "SELECT DISTINCT `a`.`i` AS `i` FROM `a` AS `a` WHERE `a`.`i` = 1",
        Iterables.get(optimized, 0).toString());
  }

  @Test
  void test2() {
    final String schemaSQL =
        "create table a (i int); "
            + "create table b (j int not null references a(i)); "
            + "create table c (k int)";
    final String sql = "select DISTINCT b.* from a join b on a.i = b.j";
    final String substitution =
        "Proj<c0>(InnerJoin<c1 c2>(Input<t0>,Input<t1>))"
            + "|Proj<c3>(Input<t2>)"
            + "|TableEq(t0,t2);PickEq(c0,c3);"
            + "PickFrom(c0,[t0]);PickFrom(c1,[t0]);PickFrom(c2,[t1]);PickFrom(c3,[t2]);"
            + "Reference(t0,c1,t1,c2)";

    final SubstitutionBank bank = SubstitutionBank.parse(singletonList(substitution));
    final ASTNode ast = ASTParser.mysql().parse(sql);
    final Schema schema = Schema.parse(MYSQL, schemaSQL);
    ast.context().setSchema(schema);
    final Set<ASTNode> optimized = OptimizerSupport.optimize(bank, schema, ast);

    assertEquals(1, optimized.size());
    assertEquals(
        "SELECT DISTINCT `b`.`j` AS `j` FROM `b` AS `b`", Iterables.get(optimized, 0).toString());
  }

  @Test
  void test3() {
    final String schemaSQL =
        "create table a (i int); "
            + "create table b (j int not null references a(i)); "
            + "create table c (k int)";
    final String sql = "select DISTINCT b.* from a join b on a.i = b.j join c on b.j = c.k";
    final String substitution =
        "Proj<c0>(InnerJoin<c1 c2>(Input<t0>,Input<t1>))"
            + "|Proj<c3>(Input<t2>)"
            + "|TableEq(t0,t2);PickEq(c0,c3);"
            + "PickFrom(c0,[t0]);PickFrom(c1,[t0]);PickFrom(c2,[t1]);PickFrom(c3,[t2]);"
            + "Reference(t0,c1,t1,c2)";

    final SubstitutionBank bank = SubstitutionBank.parse(singletonList(substitution));
    final ASTNode ast = ASTParser.mysql().parse(sql);
    final Schema schema = Schema.parse(MYSQL, schemaSQL);
    ast.context().setSchema(schema);
    final Set<ASTNode> optimized = OptimizerSupport.optimize(bank, schema, ast);

    assertEquals(1, optimized.size());
    assertEquals(
        "SELECT DISTINCT `b`.`j` AS `j` FROM `b` AS `b` INNER JOIN `c` AS `c` ON `b`.`j` = `c`.`k`",
        Iterables.get(optimized, 0).toString());
  }
}
