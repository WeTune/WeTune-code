package sjtu.ipads.wtune.stmt.mutator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.stmt.Setup;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static sjtu.ipads.wtune.stmt.TestHelper.fastRecycleIter;

public class BoolNormalizerTest {
  @BeforeAll
  static void setUp() throws ClassNotFoundException {
    Class.forName("org.sqlite.JDBC");
    Setup._default().registerAsGlobal().setup();
  }

  @Test
  @DisplayName("[Stmt.Mutator.BooleanNormalizer] simple")
  void testOne() {
    final Statement stmt = new Statement();
    stmt.setAppName("test");
    stmt.setRawSql("select * from a where a.i or (a.j is false and (a.j = a.i) is false)");
    stmt.mutate(BoolNormalizer.class);
    assertEquals(
        "SELECT * FROM `a` "
            + "WHERE `a`.`i` IS TRUE "
            + "OR NOT `a`.`j` IS TRUE "
            + "AND NOT `a`.`j` = `a`.`i`",
        stmt.parsed().toString());
  }

  @Test
  @DisplayName("[Stmt.Mutator.BooleanNormalizer] all statements")
  void testAll() {
    final List<Statement> stmts = Statement.findAll();
    for (Statement stmt : fastRecycleIter(stmts)) {
      if (stmt.parsed() == null) continue;
      stmt.mutate(BoolNormalizer.class);
    }
  }
}
