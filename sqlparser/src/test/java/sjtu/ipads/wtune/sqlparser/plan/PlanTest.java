package sjtu.ipads.wtune.sqlparser.plan;

import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sqlparser.ASTContext;
import sjtu.ipads.wtune.sqlparser.ASTParser;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.internal.ToASTTranslator;
import sjtu.ipads.wtune.sqlparser.plan.internal.ToPlanTranslator;
import sjtu.ipads.wtune.sqlparser.schema.Schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static sjtu.ipads.wtune.sqlparser.ast.ASTNode.MYSQL;

public class PlanTest {
  @Test
  void test() {
    final String schemaDef =
        ""
            + "CREATE TABLE a (x int, y int, z int);"
            + "CREATE TABLE b (i int, j int);"
            + "CREATE TABLE c (p int, q int, r int, s int)";

    final String sql =
        ""
            + "SELECT * "
            + "FROM (SELECT * FROM a) a"
            + " JOIN b ON b.i = a.x "
            + "WHERE b.j IN (SELECT `c`.`p` FROM c)";

    final Schema schema = Schema.parse(MYSQL, schemaDef);
    final ASTNode node = ASTParser.ofDb(MYSQL).parse(sql);
    final ASTContext context = node.context();
    context.setSchema(schema);

    final PlanNode plan = ToPlanTranslator.translate(node);
    final ASTNode ast = ToASTTranslator.translate(plan);
    assertEquals(
        "SELECT `a`.`x`, `a`.`y`, `a`.`z`, `b`.`i`, `b`.`j` FROM (SELECT `a`.`x`, `a`.`y`, `a`.`z` FROM `a` AS `a`) INNER JOIN `b` AS `b` ON `a`.`x` = `b`.`i` WHERE `b`.`j` IN (SELECT `c`.`p` FROM `c` AS `c`)",
        ast.toString());
  }
}
