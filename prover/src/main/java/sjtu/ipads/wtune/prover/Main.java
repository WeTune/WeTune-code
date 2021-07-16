package sjtu.ipads.wtune.prover;

import static sjtu.ipads.wtune.prover.ProverSupport.canonizeExpr;
import static sjtu.ipads.wtune.prover.ProverSupport.normalizeExpr;
import static sjtu.ipads.wtune.prover.ProverSupport.translateToExpr;

import sjtu.ipads.wtune.prover.logic.Prover;
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

    if (stmt0.toString().startsWith("shopizer")) {
      System.out.println("skip shopizer");
      return 0;
    }

    final PlanNode plan0 = makePlan(stmt0);
    final PlanNode plan1 = makePlan(stmt1);

    final Schema schema = stmt0.app().schema("base", true);
    System.out.println(translateToExpr(plan0));
    System.out.println(translateToExpr(plan1));

    final Disjunction normalForm0 = normalizeExpr(translateToExpr(plan0));
    final Disjunction normalForm1 = normalizeExpr(translateToExpr(plan1));

    final Disjunction canonicalForm0 = canonizeExpr(normalForm0, schema);
    final Disjunction canonicalForm1 = canonizeExpr(normalForm1, schema);

    System.out.println(canonicalForm0);
    System.out.println(canonicalForm1);

    final Prover prover = ProverSupport.mkProver();
    System.out.println(prover.prove(canonicalForm0, canonicalForm1));

    //    translator.assertions().forEach(System.out::println);
    //    System.out.println(expr0);
    //    System.out.println(expr1);

    //
    //    final boolean eq = ProverSupport.decideEq(canonicalForm0, canonicalForm1, ctx);
    //    System.out.println(eq);

    //    return eq ? 1 : 0;
    return 1;
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

  private static void test1() {
    final Statement stmt0 =
        Statement.make("test", "Select * From (Select i From a Where a.j > 10) As Sub", null);
    final Statement stmt1 = Statement.make("test", "Select i From a Where a.j > 10", null);
    decide(stmt0, stmt1);
  }

  public static void main(String[] args) {
    test1();
    //    test0("", true);
    //    test0("diaspora-577", true);
  }
}
