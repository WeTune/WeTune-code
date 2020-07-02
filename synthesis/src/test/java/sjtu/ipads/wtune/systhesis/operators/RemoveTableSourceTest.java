package sjtu.ipads.wtune.systhesis.operators;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.stmt.Setup;
import sjtu.ipads.wtune.stmt.resovler.TableResolver;
import sjtu.ipads.wtune.stmt.statement.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RemoveTableSourceTest {
  @BeforeAll
  static void setUp() throws ClassNotFoundException {
    Class.forName("org.sqlite.JDBC");
    Setup._default().registerAsGlobal().setup();
  }

  @Test
  @DisplayName("[Synthesis.Operator.RemoveTable]")
  void test() {
    final Statement stmt = new Statement();
    stmt.setAppName("test");
    {
      stmt.setRawSql("select 1 from a");
      stmt.resolve(TableResolver.class);
      RemoveTableSource.build("a").apply(stmt.parsed());
      assertEquals("SELECT 1", stmt.parsed().toString());
    }
    {
      stmt.setRawSql("select 1 from a join b on a.i = b.x");
      stmt.resolve(TableResolver.class);
      RemoveTableSource.build("a").apply(stmt.parsed());
      assertEquals("SELECT 1 FROM `b`", stmt.parsed().toString());
    }
    {
      stmt.setRawSql("select 1 from (select i from a) ta join b on ta.i = b.x");
      stmt.resolve(TableResolver.class);
      RemoveTableSource.build("ta").apply(stmt.parsed());
      assertEquals("SELECT 1 FROM `b`", stmt.parsed().toString());
    }
    {
      stmt.setRawSql("select 1 from (select i from a) ta join b on ta.i = b.x");
      stmt.resolve(TableResolver.class);
      RemoveTableSource.build("b").apply(stmt.parsed());
      assertEquals("SELECT 1 FROM (SELECT `i` FROM `a`) AS `ta`", stmt.parsed().toString());
    }
  }
}
