package sjtu.ipads.wtune.systhesis.operators;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.stmt.Setup;
import sjtu.ipads.wtune.stmt.attrs.TableSource;
import sjtu.ipads.wtune.stmt.statement.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sjtu.ipads.wtune.sqlparser.SQLNode.QUERY_BODY;
import static sjtu.ipads.wtune.sqlparser.SQLNode.QUERY_SPEC_FROM;
import static sjtu.ipads.wtune.sqlparser.SQLTableSource.DERIVED_SUBQUERY;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_QUERY_SCOPE;

class RenameTableSourceTest {
  @BeforeAll
  static void setUp() throws ClassNotFoundException {
    Class.forName("org.sqlite.JDBC");
    Setup._default().registerAsGlobal().setup();
  }

  @Test
  @DisplayName("[Synthesis.Operator.RenameTable]")
  void test() {
    final Statement stmt = new Statement();
    stmt.setAppName("test");
    {
      stmt.setRawSql(
          "select a.i, a.j from a left join b on a.i = b.x where a.j = 1 "
              + "and exists (select 1 from b where b.z = a.k and exists (select a.i from a)) "
              + "group by j "
              + "order by a.j");
      stmt.resolveStandard();
      final TableSource tableSource =
          stmt.parsed().get(RESOLVED_QUERY_SCOPE).tableSources().get("a");

      RenameTableSource.build(tableSource, "m", false).apply(stmt);
      Resolve.build().apply(stmt);

      assertEquals(
          "SELECT `m`.`i`, `m`.`j` FROM `a` AS `m` "
              + "LEFT JOIN `b` ON `m`.`i` = `b`.`x` "
              + "WHERE `m`.`j` = 1 "
              + "AND EXISTS (SELECT 1 FROM `b` WHERE `b`.`z` = `m`.`k` "
              + "AND EXISTS (SELECT `a`.`i` FROM `a`)) "
              + "GROUP BY `m`.`j` "
              + "ORDER BY `m`.`j`",
          stmt.parsed().toString());
      assertTrue(stmt.failedResolvers().isEmpty());
    }
    {
      stmt.setRawSql("select b.i from (select j as i from a where a.k = 3) b");
      stmt.retrofitStandard();
      final TableSource tableSource =
          stmt.parsed()
              .get(QUERY_BODY)
              .get(QUERY_SPEC_FROM)
              .get(DERIVED_SUBQUERY)
              .get(RESOLVED_QUERY_SCOPE)
              .tableSources()
              .get("a");

      RenameTableSource.build(tableSource, "m", true).apply(stmt);

      assertEquals(
          "SELECT `m`.`i` "
              + "FROM (SELECT `m`.`j` AS `i` FROM `a` AS `m` WHERE `m`.`k` = 3) AS `b`",
          stmt.parsed().toString());
    }
  }
}
