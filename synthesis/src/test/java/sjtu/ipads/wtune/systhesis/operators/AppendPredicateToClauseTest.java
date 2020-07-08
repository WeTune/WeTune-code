package sjtu.ipads.wtune.systhesis.operators;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.Setup;
import sjtu.ipads.wtune.stmt.attrs.QueryScope;
import sjtu.ipads.wtune.stmt.statement.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sjtu.ipads.wtune.sqlparser.SQLExpr.BinaryOp.*;
import static sjtu.ipads.wtune.sqlparser.SQLExpr.LiteralType.INTEGER;
import static sjtu.ipads.wtune.sqlparser.SQLExpr.*;

class AppendPredicateToClauseTest {
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
      stmt.setRawSql("select 1 from a");
      stmt.resolveStandard();

      final SQLNode pred0 = binary(columnRef("a", "i"), literal(INTEGER, 1), EQUAL);
      final SQLNode pred1 = binary(columnRef("a", "j"), literal(INTEGER, 2), LESS_THAN);
      final SQLNode pred2 = binary(columnRef("a", "k"), literal(INTEGER, 3), GREATER_OR_EQUAL);

      final Operator op0 = AppendPredicateToClause.build(pred0, QueryScope.Clause.WHERE, AND);
      final Operator op1 = AppendPredicateToClause.build(pred1, QueryScope.Clause.WHERE, OR);
      final Operator op2 = AppendPredicateToClause.build(pred2, QueryScope.Clause.HAVING, AND);
      final Operator op3 = Resolve.build();

      op0.apply(stmt);
      op1.apply(stmt);
      op2.apply(stmt);
      op3.apply(stmt);

      assertEquals(
          "SELECT 1 FROM `a` WHERE `a`.`i` = 1 OR `a`.`j` < 2 HAVING `a`.`k` >= 3",
          stmt.parsed().toString());
      assertTrue(stmt.failedResolvers().isEmpty());
    }
  }
}
