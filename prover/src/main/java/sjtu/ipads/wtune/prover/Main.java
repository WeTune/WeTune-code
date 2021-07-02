package sjtu.ipads.wtune.prover;

import static sjtu.ipads.wtune.prover.ProverSupport.canonizeExpr;
import static sjtu.ipads.wtune.prover.ProverSupport.normalizeExpr;
import static sjtu.ipads.wtune.prover.ProverSupport.translateToExpr;

import sjtu.ipads.wtune.prover.normalform.Disjunction;
import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan1.PlanSupport;
import sjtu.ipads.wtune.sqlparser.plan1.ProjNode;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.stmt.Statement;

public class Main {
  private static boolean decide(Statement stmt0, Statement stmt1) {
    System.out.println(stmt0);
    System.out.println(stmt0.parsed());
    System.out.println(stmt1.parsed());

    final Schema schema = stmt0.app().schema("base", true);
    final PlanNode plan0 = PlanSupport.assemblePlan(stmt0.parsed(), schema);
    final PlanNode plan1 = PlanSupport.assemblePlan(stmt1.parsed(), schema);
    PlanSupport.disambiguate(plan0);
    PlanSupport.disambiguate(plan1);

    ((ProjNode) plan1.predecessors()[0].predecessors()[0]).setExplicitDistinct(true);

    final Disjunction normalForm0 = normalizeExpr(translateToExpr(plan0));
    final Disjunction normalForm1 = normalizeExpr(translateToExpr(plan1));

    final DecisionContext ctx = DecisionContext.make(schema, normalForm0, normalForm1);
    final Disjunction canonicalForm0 = canonizeExpr(normalForm0, ctx);
    final Disjunction canonicalForm1 = canonizeExpr(normalForm1, ctx);

    System.out.println(canonicalForm0);
    System.out.println(canonicalForm1);

    final boolean eq = ProverSupport.decideEq(canonicalForm0, canonicalForm1, ctx);
    System.out.println(eq);

    //    return eq;
    return false;
  }

  private static void test0(String latch, boolean single) {
    boolean start = latch.isEmpty();
    int failed = 0, succeed = 0;

    for (Statement rewritten : Statement.findAllRewritten()) {
      if (latch.equals(rewritten.toString())) start = true;
      if (!start) continue;

      final Statement original = rewritten.original();

      final boolean eq = decide(original, rewritten);

      if (eq) succeed += 1;
      else failed += 1;

      if (single) break;
    }

    System.out.println("#success: " + succeed);
    System.out.println("#failure: " + failed);
  }

  public static void main(String[] args) {
    //    test0("", false);
    test0("broadleaf-199", true);
  }
}
