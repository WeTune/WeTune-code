package sjtu.ipads.wtune.stmt;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.dao.StatementDao;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class ParserTest {
  @BeforeAll
  static void setUp() throws ClassNotFoundException {
    Class.forName("org.sqlite.JDBC");
    Setup._default().registerAsGlobal().setup();
  }

  @AfterAll
  static void toreDown() {
    StatementDao.getGlobal().close();
  }

  @Test
  @DisplayName("[stmt] parsing all statements")
  void test() {
    final List<Statement> stmts = Statement.findAll();

    for (Statement stmt : stmts) {
      final SQLNode parsed = stmt.parsed();
      if (parsed == null) continue;
      assertFalse(parsed.toString().contains("<??>"));
    }
  }
}
