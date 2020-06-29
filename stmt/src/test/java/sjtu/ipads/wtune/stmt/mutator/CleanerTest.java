package sjtu.ipads.wtune.stmt.mutator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.Setup;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CleanerTest {
  @BeforeAll
  static void setUp() throws ClassNotFoundException {
    Class.forName("org.sqlite.JDBC");
    Setup._default().registerAsGlobal().setup();
  }

  @Test
  @DisplayName("[stmt.mutator.cleaner] simple statements")
  void testSimple() {
    Statement stmt = new Statement();
    stmt.setAppName("broadleaf");
    {
      stmt.setRawSql("select a from t where 1=1");
      stmt.mutate(Cleaner.class);
      assertEquals("SELECT `a` FROM `t`", stmt.parsed().toString());
    }
    {
      stmt.setRawSql("select a from t where true and b");
      stmt.mutate(Cleaner.class);
      assertEquals("SELECT `a` FROM `t` WHERE `b`", stmt.parsed().toString());
    }
    {
      stmt.setRawSql("select a from t where 1+1 = 2 and (c or 1 between 0 and 2)");
      stmt.mutate(Cleaner.class);
      assertEquals("SELECT `a` FROM `t` WHERE `c`", stmt.parsed().toString());
    }
    {
      stmt.setRawSql("select a from t where now() = 0 or rand() = 10 or field(k, 1, 2) = 3");
      stmt.mutate(Cleaner.class);
      assertEquals(
          "SELECT `a` FROM `t` WHERE RAND() = 10 OR FIELD(`k`, 1, 2) = 3",
          stmt.parsed().toString());
    }
  }

  @Test
  @DisplayName("[stmt.mutator.cleaner] all statements")
  void testAll() {
    final List<Statement> stmts = Statement.findAll();
    for (Statement stmt : stmts) {
      if (stmt.parsed() == null) continue;
      final String original = stmt.parsed().toString();
      stmt.mutate(Cleaner.class);
      final String modified = stmt.parsed().toString();
      assertFalse(modified.contains("<??>"));
      assertTrue(!original.contains("1 = 1") || !modified.contains("1 = 1"));
      assertTrue(!original.contains("1 = 0") || !modified.contains("1 = 0"));
    }
  }
}
