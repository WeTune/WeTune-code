package sjtu.ipads.wtune.stmt.resolver;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.stmt.Setup;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.List;

import static sjtu.ipads.wtune.stmt.TestHelper.fastRecycleIter;

class ParamResolverTest {

  @BeforeAll
  static void setUp() throws ClassNotFoundException {
    Class.forName("org.sqlite.JDBC");
    Setup._default().registerAsGlobal().setup();
  }

  @Test
  @DisplayName("[stmt.resolver.column] all statements")
  void test() {
    final List<Statement> stmts = Statement.findAll();

    for (Statement stmt : fastRecycleIter(stmts)) {
      if (stmt.parsed() == null) continue;
      stmt.retrofitStandard();
      stmt.resolve(ParamResolver.class);
    }
  }
}
