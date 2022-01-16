package sjtu.ipads.wtune.sql.support.action;

import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sql.ast.SqlContext;
import sjtu.ipads.wtune.sql.ast.SqlNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static sjtu.ipads.wtune.sql.TestHelper.parseSql;

public class NormalizationTest {
  @Test
  void testRemoveBoolConstant() {
    {
      final SqlNode sql = parseSql("select a from t where 1=1");
      Clean.clean(sql);
      assertEquals("SELECT `a` FROM `t`", sql.toString());
    }
    {
      final SqlNode sql = parseSql("select a from t where true and b");
      Clean.clean(sql);
      assertEquals("SELECT `a` FROM `t` WHERE `b`", sql.toString());
    }
    {
      final SqlNode sql = parseSql("select a from t where 1+1 = 2 and (c or 1 between 0 and 2)");
      Clean.clean(sql);
      assertEquals("SELECT `a` FROM `t` WHERE `c`", sql.toString());
    }
    {
      final SqlNode sql =
          parseSql("select a from t where now() = 0 or rand() = 10 or field(k, 1, 2) = 3");
      Clean.clean(sql);
      assertEquals("SELECT `a` FROM `t` WHERE RAND() = 10 OR FIELD(`k`, 1, 2) = 3", sql.toString());
    }
  }

  @Test
  void testRemoveTextFunc() {
    final SqlNode sql = parseSql(("select a from t where a like concat('%', concat('1', '%'))"));
    Clean.clean(sql);
    assertEquals("SELECT `a` FROM `t` WHERE `a` LIKE '%1%'", sql.toString());
  }

  @Test
  void testNormalizeTuple() {
    final SqlNode sql = parseSql("select a from t where (a, b) in ((1,2),(3,4))");
    NormalizeTuple.normalize(sql);
    assertEquals("SELECT `a` FROM `t` WHERE (`a`, `b`) IN (?)", sql.toString());
  }

  @Test
  void testNormalizeBool() {
    final SqlNode sql =
        parseSql("select * from a where a.i or (a.j is false and (a.j = a.i) is false)");
    NormalizeBool.normalize(sql);
    assertEquals(
        "SELECT * FROM `a` WHERE `a`.`i` = TRUE OR NOT `a`.`j` = TRUE AND NOT `a`.`j` = `a`.`i`",
        sql.toString());
  }

  @Test
  void testNormalizeJoinCondition() {
    final SqlNode sql =
        parseSql(
            "select * from a join b on a.i = b.x and a.j=3 join c on b.y=c.v and b.z=c.w and c.u<10 where a.j=b.y");
    NormalizeJoinCond.normalize(sql);
    assertEquals(
        "SELECT * FROM `a` INNER JOIN `b` ON `a`.`i` = `b`.`x` AND `a`.`j` = `b`.`y` INNER JOIN `c` ON `b`.`y` = `c`.`v` AND `b`.`z` = `c`.`w` WHERE `a`.`j` = 3 AND `c`.`u` < 10",
        sql.toString());
  }

  @Test
  void testNormalizeConstantTable() {
    final SqlNode sql =
        parseSql(
            "select sub.j from a inner join (select 1 as j) as sub on a.i = sub.j where sub.j = 1 order by sub.j");
    InlineLiteralTable.normalize(sql);
    Clean.clean(sql);
    final SqlContext ctx = sql.context();
    ctx.deleteDetached(ctx.root());
    ctx.compact();
    assertEquals("SELECT 1 FROM `a` WHERE `a`.`i` = 1", sql.toString());
  }
}
