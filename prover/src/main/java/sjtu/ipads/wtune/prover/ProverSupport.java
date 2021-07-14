package sjtu.ipads.wtune.prover;

import sjtu.ipads.wtune.prover.normalform.Canonization;
import sjtu.ipads.wtune.prover.normalform.Disjunction;
import sjtu.ipads.wtune.prover.normalform.Normalization;
import sjtu.ipads.wtune.prover.uexpr.UExpr;
import sjtu.ipads.wtune.prover.uexpr.UExprTranslator;
import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;
import sjtu.ipads.wtune.sqlparser.schema.Schema;

public class ProverSupport {
  public static UExpr translateToExpr(PlanNode plan) {
    return UExprTranslator.translate(plan);
  }

  public static Disjunction normalizeExpr(UExpr uExpr) {
    return Normalization.normalize(uExpr);
  }

  public static Disjunction canonizeExpr(Disjunction disjunction, Schema schema) {
    return Canonization.canonize(disjunction, schema);
  }
}
