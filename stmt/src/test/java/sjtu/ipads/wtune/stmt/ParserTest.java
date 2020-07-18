package sjtu.ipads.wtune.stmt;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.context.AppContext;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static sjtu.ipads.wtune.stmt.TestHelper.fastRecycleIter;

public class ParserTest {
  @BeforeAll
  static void setUp() throws ClassNotFoundException {
    Class.forName("org.sqlite.JDBC");
    Setup._default().registerAsGlobal().setup();
  }

  @Test
  @DisplayName("[stmt] parsing all schema")
  void testSchema() {
    Statement.findAll().forEach(Statement::appContext);
    for (AppContext appContext : AppContext.all()) appContext.schema();
  }

  @Test
  @DisplayName("[stmt] parsing all statements")
  void testStatement() {
    final List<Statement> stmts = Statement.findAll();

    for (Statement stmt : fastRecycleIter(stmts)) {
      final SQLNode parsed = stmt.parsed();
      if (parsed == null) {
        System.out.println(stmt.toString());
        continue;
      }
      assertFalse(parsed.toString().contains("<??>"));
      final Statement stmt1 = new Statement();
      stmt1.setAppName(stmt.appName());
      stmt1.setRawSql(stmt.parsed().toString());
      assertNotNull(stmt1.parsed(), stmt.toString());
      assertEquals(stmt.parsed().toString(), stmt1.parsed().toString(), stmt.toString());
    }
  }
}
