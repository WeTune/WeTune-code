package sjtu.ipads.wtune.sqlparser.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static sjtu.ipads.wtune.sqlparser.ast.ASTNode.MYSQL;

import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sqlparser.ASTContext;
import sjtu.ipads.wtune.sqlparser.ASTParser;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.schema.Schema;

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

    final PlanNode plan = ToPlanTranslator.toPlan(node);
    final ASTNode ast = ToASTTranslator.toAST(plan);
    assertEquals(
        "SELECT * FROM (SELECT `a`.`x` AS `x`, `a`.`y` AS `y`, `a`.`z` AS `z` FROM `a` AS `a`) AS `a` INNER JOIN `b` AS `b` ON `a`.`x` = `b`.`i` WHERE `b`.`j` IN (SELECT `c`.`p` AS `p` FROM `c` AS `c`)",
        ast.toString());
  }
}
