package sjtu.ipads.wtune.prover;

import static sjtu.ipads.wtune.prover.ProverSupport.canonizeExpr;
import static sjtu.ipads.wtune.prover.ProverSupport.normalizeExpr;
import static sjtu.ipads.wtune.prover.ProverSupport.translateToExpr;

import sjtu.ipads.wtune.common.utils.Commons;
import sjtu.ipads.wtune.prover.normalform.Disjunction;
import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan1.PlanSupport;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.stmt.support.Workflow;

public class Main {
  private static PlanNode makePlan(Statement stmt) {
    final Schema schema = stmt.app().schema("base", true);
    stmt.parsed().context().setSchema(schema);

    Workflow.normalize(stmt.parsed());

    System.out.println(stmt.parsed());

    final PlanNode plan = PlanSupport.assemblePlan(stmt.parsed(), schema);
    PlanSupport.disambiguate(plan);

    return plan;
  }

  private static int decide(Statement stmt0, Statement stmt1) {
    System.out.println(stmt0);

    if (Commons.countOccurrences(stmt0.parsed().toString(), "LEFT JOIN") >= 2) {
      System.out.println("skipped due to too many LEFT JOIN");
      return 0;
    }

    final PlanNode plan0 = makePlan(stmt0);
    final PlanNode plan1 = makePlan(stmt1);

    final Schema schema = stmt0.app().schema("base", true);

    final Disjunction normalForm0 = normalizeExpr(translateToExpr(plan0));
    final Disjunction normalForm1 = normalizeExpr(translateToExpr(plan1));

    final DecisionContext ctx = DecisionContext.make(schema, normalForm0, normalForm1);
    final Disjunction canonicalForm0 = canonizeExpr(normalForm0, ctx);
    final Disjunction canonicalForm1 = canonizeExpr(normalForm1, ctx);

    System.out.println(canonicalForm0);
    System.out.println(canonicalForm1);

    final boolean eq = ProverSupport.decideEq(canonicalForm0, canonicalForm1, ctx);
    System.out.println(eq);

    return eq ? 1 : 0;
  }

  private static void test0(String latch, boolean single) {
    boolean start = latch.isEmpty();
    int failed = 0, succeed = 0, exception = 0;

    for (Statement rewritten : Statement.findAllRewritten()) {
      if (latch.equals(rewritten.toString())) start = true;
      if (!start) continue;

      final Statement original = rewritten.original();

      final int res = decide(original, rewritten);

      if (res == 1) succeed += 1;
      else if (res == 0) failed += 1;
      else exception += 1;

      if (single) break;
    }

    System.out.println("#success: " + succeed);
    System.out.println("#failure: " + failed);
    System.out.println("#exception: " + exception);
  }

  public static void main(String[] args) {
    test0("", false);
    //    test0("diaspora-577", true);
    //    test0("discourse-4290", true);
  }
}
