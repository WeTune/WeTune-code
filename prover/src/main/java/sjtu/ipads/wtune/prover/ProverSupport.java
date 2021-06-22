package sjtu.ipads.wtune.prover;

import sjtu.ipads.wtune.prover.expr.UExpr;
import sjtu.ipads.wtune.prover.normalform.Disjunction;
import sjtu.ipads.wtune.prover.normalform.Normalization;
import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;

public class ProverSupport {
  public static UExpr translateExpr(PlanNode plan) {
    return ExprTranslator.translate(plan);
  }

  public static Disjunction normalize(UExpr uExpr, DecisionContext ctx) {
    return Normalization.normalize(uExpr, ctx);
  }
}
