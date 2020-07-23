package sjtu.ipads.wtune.systhesis.predicate;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.stmt.Setup;
import sjtu.ipads.wtune.stmt.statement.Statement;
import sjtu.ipads.wtune.systhesis.SynthesisContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PredicateMutationTest {
  @BeforeAll
  static void setUp() throws ClassNotFoundException {
    Class.forName("org.sqlite.JDBC");
    Setup._default().registerAsGlobal().setup();
  }

  @Test
  @DisplayName("[Synthesis.Operator.PredicateMutation]")
  void test() {
    final Statement stmt0 = new Statement();
    stmt0.setAppName("test");
    stmt0.setRawSql(
        "select 1 from c where c.u = 0 or c.u between 1 and 2 and c.v like '1' or c.w = 1.0");
    //        "select 1 from c where c.u = 0");
    stmt0.retrofitStandard();

    final Statement stmt1 = new Statement();
    stmt1.setAppName("test");
    stmt1.setRawSql("select 1 from d where d.p > 10 or d.p < 15 and match d.q against ('123')");
    //    stmt1.setRawSql("select 1 from d where d.p > 10 or d.p < 15");
    stmt1.retrofitStandard();

    final SynthesisContext ctx = new SynthesisContext(stmt0);
    ctx.setReferenceStmt(stmt1);

    final PredicateMutation mutation = new PredicateMutation(ctx, Integer.MAX_VALUE, 2);
    mutation.setNext(ctx.collector());

    mutation.feed(stmt0);
    final List<Statement> output = ctx.candidates();
    assertEquals(18, output.size());
  }
}
