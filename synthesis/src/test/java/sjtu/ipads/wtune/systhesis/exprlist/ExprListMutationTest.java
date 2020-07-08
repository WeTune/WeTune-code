package sjtu.ipads.wtune.systhesis.exprlist;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.stmt.Setup;
import sjtu.ipads.wtune.stmt.statement.Statement;
import sjtu.ipads.wtune.systhesis.OptContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExprListMutationTest {
  @BeforeAll
  static void setUp() throws ClassNotFoundException {
    Class.forName("org.sqlite.JDBC");
    Setup._default().registerAsGlobal().setup();
  }

  @Test
  @DisplayName("[Synthesis.Operator.ExprListMutation] distinct")
  void testReduceDistinct() {
    final Statement stmt0 = new Statement();
    stmt0.setAppName("test");
    stmt0.setRawSql(
        "select distinct a.i from a inner join (select distinct x from b) x "
            + "where exists (select distinct u from c)");
    stmt0.retrofitStandard();

    final OptContext ctx = new OptContext(stmt0);
    final ExprListMutation mutation = new ExprListMutation();
    mutation.setNext(ctx.outputStage());

    mutation.feed(stmt0);
    final List<Statement> output = ctx.output();
    assertEquals(8, output.size());
  }

  @Test
  @DisplayName("[Synthesis.Operator.ExprListMutation] order by")
  void testReduceOrderBy() {
    final Statement stmt0 = new Statement();
    stmt0.setAppName("test");
    stmt0.setRawSql(
        "select a.i from a inner join (select x from b order by b.y) x "
            + "where exists (select u from c order by c.v) order by a.j");
    stmt0.retrofitStandard();

    final OptContext ctx = new OptContext(stmt0);
    final ExprListMutation mutation = new ExprListMutation();
    mutation.setNext(ctx.outputStage());

    mutation.feed(stmt0);
    final List<Statement> output = ctx.output();
    assertEquals(4, output.size());
  }
}
