package sjtu.ipads.wtune.prover;

import sjtu.ipads.wtune.prover.decision.DecisionProcedure;
import sjtu.ipads.wtune.prover.expr.UExpr;
import sjtu.ipads.wtune.prover.normalform.Canonization;
import sjtu.ipads.wtune.prover.normalform.Disjunction;
import sjtu.ipads.wtune.prover.normalform.Normalization;
import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;

public class ProverSupport {
  public static UExpr translateToExpr(PlanNode plan) {
    return ExprTranslator.translate(plan);
  }

  public static Disjunction normalizeExpr(UExpr uExpr) {
    return Normalization.normalize(uExpr);
  }

  public static Disjunction canonizeExpr(Disjunction disjunction, DecisionContext ctx) {
    return Canonization.canonize(disjunction, ctx);
  }

  public static boolean decideEq(Disjunction x, Disjunction y, DecisionContext ctx) {
    return DecisionProcedure.decide(x, y, ctx) != null;
  }

  public static boolean decideEq(PlanNode p0, PlanNode p1) {
    if (p0.context().schema() != p1.context().schema()) return false;

    final UExpr e0 = translateToExpr(p0), e1 = translateToExpr(p1);
    final Disjunction normalForm0 = normalizeExpr(e0);
    final Disjunction normalForm1 = normalizeExpr(e1);

    final DecisionContext ctx =
        DecisionContext.make(p0.context().schema(), normalForm0, normalForm1);
    final Disjunction canonicalForm0 = canonizeExpr(normalForm0, ctx);
    final Disjunction canonicalForm1 = canonizeExpr(normalForm1, ctx);

    return decideEq(canonicalForm0, canonicalForm1, ctx);
  }
}
