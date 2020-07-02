package sjtu.ipads.wtune.systhesis.operators;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLTableSource;
import sjtu.ipads.wtune.stmt.Setup;
import sjtu.ipads.wtune.stmt.attrs.QueryScope;
import sjtu.ipads.wtune.stmt.statement.Statement;

import static org.junit.jupiter.api.Assertions.*;
import static sjtu.ipads.wtune.sqlparser.SQLExpr.*;
import static sjtu.ipads.wtune.sqlparser.SQLExpr.BinaryOp.*;
import static sjtu.ipads.wtune.sqlparser.SQLExpr.BinaryOp.AND;
import static sjtu.ipads.wtune.sqlparser.SQLExpr.LiteralType.INTEGER;
import static sjtu.ipads.wtune.sqlparser.SQLTableSource.simple;

class AddTableSourceTest {
  @BeforeAll
  static void setUp() throws ClassNotFoundException {
    Class.forName("org.sqlite.JDBC");
    Setup._default().registerAsGlobal().setup();
  }

  @Test
  @DisplayName("[Synthesis.Operator.AddTable]")
  void test() {
    final Statement stmt = new Statement();
    stmt.setAppName("test");
    {
      stmt.setRawSql("select 1");
      stmt.resolveStandard();

      final SQLNode ta0 = simple("a", "a0");
      final SQLNode tb0 = simple("b", "b0");
      final SQLNode ta1 = simple("a", "a1");

      final SQLNode cond0 = binary(columnRef("a0", "i"), columnRef("b0", "x"), EQUAL);
      final SQLNode cond1 = binary(columnRef("b0", "y"), columnRef("a1", "j"), EQUAL);

      final Operator op0 = AddTableSource.build(ta0, null, null);
      final Operator op1 = AddTableSource.build(tb0, cond0, SQLTableSource.JoinType.INNER_JOIN);
      final Operator op2 = AddTableSource.build(ta1, cond1, SQLTableSource.JoinType.LEFT_JOIN);
      final Operator op3 = Resolve.build();

      op0.apply(stmt);
      op1.apply(stmt);
      op2.apply(stmt);
      op3.apply(stmt);

      assertEquals(
          "SELECT 1 FROM `a` AS `a0` "
              + "INNER JOIN `b` AS `b0` ON `a0`.`i` = `b0`.`x` "
              + "LEFT JOIN `a` AS `a1` ON `b0`.`y` = `a1`.`j`",
          stmt.parsed().toString());
      assertTrue(stmt.failedResolvers().isEmpty());
    }
  }
}
