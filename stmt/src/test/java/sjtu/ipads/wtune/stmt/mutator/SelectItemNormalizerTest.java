package sjtu.ipads.wtune.stmt.mutator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.stmt.Setup;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static sjtu.ipads.wtune.stmt.TestHelper.fastRecycleIter;

class SelectItemNormalizerTest {

  @BeforeAll
  static void setUp() throws ClassNotFoundException {
    Class.forName("org.sqlite.JDBC");
    Setup._default().registerAsGlobal().setup();
  }

  @Test
  @DisplayName("[Stmt.Mutator.SelectItem] simple statement")
  void test() {
    final Statement stmt = new Statement();
    stmt.setAppName("test");
    stmt.setRawSql(
        "select a.i from a join b "
            + "join (select distinct u, v from c) c "
            + "join (select p from d) d "
            + "where exists (select q from d)");
    stmt.retrofitStandard();
    stmt.mutate(SelectItemNormalizer.class);
    assertEquals(
        "SELECT `a`.`i` AS `_primary_0`, `b`.`x` AS `_primary_1`, `c`.`u` AS `_primary_2`, `d`.`_primary_0` AS `_primary_3`, `d`.`_primary_1` AS `_primary_4`, `d`.`_primary_2` AS `_primary_5` FROM `a` INNER JOIN `b` INNER JOIN (SELECT DISTINCT `u`, `v` FROM `c`) AS `c` INNER JOIN (SELECT `p`, `d`.`p` AS `_primary_0`, `d`.`q` AS `_primary_1`, `d`.`r` AS `_primary_2` FROM `d`) AS `d` WHERE EXISTS (SELECT `q` FROM `d`)",
        stmt.parsed().toString());
  }

  @Test
  @DisplayName("[Stmt.Mutator.SelectItem] all statements")
  void testAll() {
    final List<Statement> stmts = Statement.findAll();
    for (Statement stmt : fastRecycleIter(stmts)) {
      stmt.retrofitStandard();
      stmt.mutate(SelectItemNormalizer.class);
      assertFalse(stmt.parsed().toString().contains("SELECT  FROM"));
    }
  }
}
