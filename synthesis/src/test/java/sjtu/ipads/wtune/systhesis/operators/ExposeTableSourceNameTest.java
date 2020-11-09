package sjtu.ipads.wtune.systhesis.operators;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.stmt.Setup;
import sjtu.ipads.wtune.stmt.attrs.TableSource;
import sjtu.ipads.wtune.stmt.statement.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_QUERY_SCOPE;

class ExposeTableSourceNameTest {
  @BeforeAll
  static void setUp() throws ClassNotFoundException {
    Class.forName("org.sqlite.JDBC");
    Setup._default().registerAsGlobal().setup();
  }

  @Test
  @DisplayName("[Synthesis.Operator.InlineRefName]")
  void test() {
    final Statement stmt = new Statement();
    stmt.setAppName("test");
    {
      stmt.setRawSql(
          "select b.m from (select a.i AS m, a.j AS n, a.k AS o from a where a.i = 0) b "
              + "where b.n = 3 and exists (select 1 from a where a.j = b.o)");
      stmt.retrofitStandard();
      final TableSource target = stmt.parsed().get(RESOLVED_QUERY_SCOPE).tableSources().get("b");
      ExposeTableSourceName.build(target).apply(stmt.parsed());
      assertEquals(
          "SELECT `b`.`i` AS `m` FROM "
              + "(SELECT `a`.`i` AS `m`, `a`.`j` AS `n`, `a`.`k` AS `o` "
              + "FROM `a` WHERE `a`.`i` = 0) AS `b` "
              + "WHERE `b`.`j` = 3 "
              + "AND EXISTS (SELECT 1 FROM `a` WHERE `a`.`j` = `b`.`k`)",
          stmt.parsed().toString());
    }
  }
}
