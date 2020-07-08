package sjtu.ipads.wtune.systhesis.operators;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.Setup;
import sjtu.ipads.wtune.stmt.resovler.BoolExprResolver;
import sjtu.ipads.wtune.stmt.resovler.ColumnResolver;
import sjtu.ipads.wtune.stmt.resovler.IdResolver;
import sjtu.ipads.wtune.stmt.statement.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static sjtu.ipads.wtune.sqlparser.SQLExpr.*;
import static sjtu.ipads.wtune.sqlparser.SQLNode.QUERY_BODY;
import static sjtu.ipads.wtune.sqlparser.SQLNode.QUERY_SPEC_WHERE;

class DropPredicateTest {
  @BeforeAll
  static void setUp() throws ClassNotFoundException {
    Class.forName("org.sqlite.JDBC");
    Setup._default().registerAsGlobal().setup();
  }

  @Test
  @DisplayName("[Synthesis.Operator.RemovePredicate]")
  void test() {
    final Statement stmt = new Statement();
    stmt.setAppName("test");
    {
      stmt.setRawSql(
          "select 1 from a where a.i = 1 and a.j = 3 "
              + "or a.k in (select 1 from b where b.x = 5)");
      stmt.resolve(IdResolver.class);
      stmt.resolve(BoolExprResolver.class);
      stmt.resolve(ColumnResolver.class);

      final SQLNode whereClause = stmt.parsed().get(QUERY_BODY).get(QUERY_SPEC_WHERE);
      final SQLNode target0 = whereClause.get(BINARY_RIGHT);
      final SQLNode target1 = whereClause.get(BINARY_LEFT).get(BINARY_LEFT);

      DropPredicate.build(target0).apply(stmt.parsed());
      DropPredicate.build(target1).apply(stmt.parsed());

      assertEquals("SELECT 1 FROM `a` WHERE `a`.`j` = 3", stmt.parsed().toString());
    }
  }
}
