package sjtu.ipads.wtune.stmt.resolver;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.stmt.Setup;
import sjtu.ipads.wtune.stmt.resovler.TableResolver;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.List;

public class TableResolverTest {

  @BeforeAll
  static void setUp() throws ClassNotFoundException {
    Class.forName("org.sqlite.JDBC");
    Setup._default().registerAsGlobal().setup();
  }

  @Test
  @DisplayName("[stmt.resolver.table] all statements")
  void test() {
    final List<Statement> stmts = Statement.findAll();
    for (Statement stmt : stmts) {
      if (stmt.parsed() == null) continue;
      stmt.resolve(TableResolver.class);
    }
  }
}
