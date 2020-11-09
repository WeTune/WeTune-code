package sjtu.ipads.wtune.systhesis.predicate;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.Setup;
import sjtu.ipads.wtune.stmt.statement.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sjtu.ipads.wtune.sqlparser.SQLNode.QUERY_BODY;
import static sjtu.ipads.wtune.sqlparser.SQLNode.QUERY_SPEC_WHERE;

class DisplacePredicateTest {
  @BeforeAll
  static void setUp() throws ClassNotFoundException {
    Class.forName("org.sqlite.JDBC");
    Setup._default().registerAsGlobal().setup();
  }

  @Test
  @DisplayName("[Synthesis.Operator.AddPredicate]")
  void test() {
    final Statement stmt = new Statement();
    stmt.setAppName("test");
    {
      stmt.setRawSql("select 1 from a where a.i = 0");
      stmt.retrofitStandard();
      final SQLNode rep = stmt.parsed().get(QUERY_BODY).get(QUERY_SPEC_WHERE);

      stmt.setRawSql("select 1 from b where b.x > 10");
      stmt.retrofitStandard();
      final SQLNode target = stmt.parsed().get(QUERY_BODY).get(QUERY_SPEC_WHERE);

      assertTrue(DisplacePredicate.canDisplace(target, rep));
      final DisplacePredicate mutator = new DisplacePredicate(target, rep);
      mutator.modifyAST(stmt.parsed());

      assertEquals("SELECT 1 FROM `b` WHERE `b`.`x` = 0", stmt.parsed().toString());
    }
  }
}
