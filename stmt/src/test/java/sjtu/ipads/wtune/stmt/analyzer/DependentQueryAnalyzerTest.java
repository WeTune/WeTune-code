package sjtu.ipads.wtune.stmt.analyzer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.Setup;
import sjtu.ipads.wtune.stmt.dao.StatementDao;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.List;

import static sjtu.ipads.wtune.stmt.TestHelper.fastRecycleIter;

class DependentQueryAnalyzerTest {
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
  @DisplayName("[Stmt.Analyzer.DependentQuery] all statements")
  void test() {
    final List<Statement> stmts = Statement.findAll();

    for (Statement stmt : fastRecycleIter(stmts)) {
      final SQLNode parsed = stmt.parsed();
      if (parsed == null) continue;
      stmt.analyze(DependentQueryAnalyzer.class);
    }
  }
}
