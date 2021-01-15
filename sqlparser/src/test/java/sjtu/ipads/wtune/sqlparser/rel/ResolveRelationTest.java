package sjtu.ipads.wtune.sqlparser.rel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sqlparser.SQLParser;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static sjtu.ipads.wtune.common.utils.FuncUtils.func;
import static sjtu.ipads.wtune.sqlparser.SQLContext.resolveRelation;
import static sjtu.ipads.wtune.sqlparser.ast.SQLNode.MYSQL;
import static sjtu.ipads.wtune.sqlparser.ast.SQLVisitor.topDownVisit;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprType.COLUMN_REF;

public class ResolveRelationTest {
  @Test
  void test() {
    final String schemaDef =
        ""
            + "CREATE TABLE a (x int, y int, z int);"
            + "CREATE TABLE b (i int, j int, k int);"
            + "CREATE TABLE c (p int, q int, r int)";
    final Schema schema = Schema.parse(MYSQL, schemaDef);

    final String sql =
        ""
            + "SELECT * "
            + "FROM (SELECT * FROM a) a"
            + " JOIN b ON a.x = b.i "
            + "WHERE EXISTS (SELECT *, a.*, `c`.`p` FROM c)";
    final SQLNode ast = resolveRelation(SQLParser.ofDb(MYSQL).parse(sql), schema);

    assertEquals(
        ""
            + "SELECT `a`.`x` AS `x`,"
            + " `a`.`y` AS `y`,"
            + " `a`.`z` AS `z`,"
            + " `b`.`i` AS `i`,"
            + " `b`.`j` AS `j`,"
            + " `b`.`k` AS `k` "
            + "FROM"
            + " (SELECT"
            + " `a`.`x` AS `x`,"
            + " `a`.`y` AS `y`,"
            + " `a`.`z` AS `z`"
            + " FROM `a`) AS `a`"
            + " INNER JOIN `b`"
            + " ON `a`.`x` = `b`.`i` "
            + "WHERE EXISTS (SELECT"
            + " `c`.`p` AS `p`,"
            + " `c`.`q` AS `q`,"
            + " `c`.`r` AS `r`,"
            + " `a`.`x` AS `x`,"
            + " `a`.`y` AS `y`,"
            + " `a`.`z` AS `z`,"
            + " `c`.`p` AS `p`"
            + " FROM `c`)",
        ast.toString());
    ast.accept(topDownVisit(func(Relation::of).then(Assertions::assertNotNull)));
    ast.accept(topDownVisit(COLUMN_REF, func(Attribute::of).then(Assertions::assertNotNull)));
  }
}
