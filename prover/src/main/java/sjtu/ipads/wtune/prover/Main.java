package sjtu.ipads.wtune.prover;

import static sjtu.ipads.wtune.prover.ProverSupport.normalize;

import sjtu.ipads.wtune.prover.decision.DecisionProcedure;
import sjtu.ipads.wtune.prover.expr.UExpr;
import sjtu.ipads.wtune.prover.normalform.Disjunction;
import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan1.PlanSupport;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.stmt.Statement;

public class Main {
  public static void main(String[] args) {
    final String latch = "";
    boolean start = latch.isEmpty();

    int failed = 0, succeed = 0;
    for (Statement rewritten : Statement.findAllRewritten()) {
      if (latch.equals(rewritten.toString())) start = true;
      if (!start) continue;

      final Statement original = rewritten.original();
      System.out.println(original);

      final Schema schema = original.app().schema("base");
      try {
        final PlanNode plan0 = PlanSupport.assemblePlan(original.parsed(), schema);
        final PlanNode plan1 = PlanSupport.assemblePlan(rewritten.parsed(), schema);
        PlanSupport.disambiguate(plan0);
        PlanSupport.disambiguate(plan1);

        final UExpr expr0 = ExprTranslator.translate(plan0);
        final UExpr expr1 = ExprTranslator.translate(plan1);

        final DecisionContext ctx = DecisionContext.make(expr0, expr1);
        ctx.setSchema(original.app().schema("base"));

        final Disjunction canonicalForm0 = normalize(expr0, ctx);
        final Disjunction canonicalForm1 = normalize(expr1, ctx);

        System.out.println(canonicalForm0);
        System.out.println(canonicalForm1);

        final boolean eq = DecisionProcedure.decide(ctx, canonicalForm0, canonicalForm1) != null;
        System.out.println(eq);
        if (eq) succeed += 1;
        else failed += 1;

        //        System.out.println(plan0);
        //        System.out.println(plan1);
      } catch (Throwable ex) {
        System.out.println(original);
        throw ex;
      }
    }
    System.out.println("success: " + succeed);
    System.out.println("failure: " + failed);
  }
}
