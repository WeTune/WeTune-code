package sjtu.ipads.wtune.stmt.analyzer;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.Setup;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.List;

import static sjtu.ipads.wtune.stmt.TestHelper.fastRecycleIter;

class ImpliedForeignKeyAnalyzerTest {
  @BeforeAll
  static void setUp() throws ClassNotFoundException {
    Class.forName("org.sqlite.JDBC");
    Setup._default().registerAsGlobal().setup();
  }

  @Test
  @DisplayName("[Stmt.Analyzer.ImpliedForeignKey] all statements")
  void test() {
    final List<Statement> stmts = Statement.findAll();

    for (Statement stmt : fastRecycleIter(stmts)) {
      final SQLNode parsed = stmt.parsed();
      if (parsed == null) continue;
      stmt.analyze(ImpliedForeignKeyAnalyzer.class);
    }

    //        for (AppContext appContext : AppContext.all()) {
    //          final Set<Column> keys = appContext.get(IMPLIED_FOREIGN_KEYS);
    //          if (keys == null) continue;
    //
    //          if (!keys.isEmpty()) {
    //            System.out.println(">" + appContext.name() + ":");
    //            System.out.println("  " + String.join("\n  ", listMap(Column::toString, keys)));
    //          }
    //        }
  }
}
