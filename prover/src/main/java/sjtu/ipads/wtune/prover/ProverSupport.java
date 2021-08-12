package sjtu.ipads.wtune.prover;

import sjtu.ipads.wtune.prover.logic.LogicCtx;
import sjtu.ipads.wtune.prover.logic.LogicProver;
import sjtu.ipads.wtune.prover.normalform.Canonization;
import sjtu.ipads.wtune.prover.normalform.Disjunction;
import sjtu.ipads.wtune.prover.normalform.Normalization;
import sjtu.ipads.wtune.prover.uexpr.UExpr;
import sjtu.ipads.wtune.prover.uexpr.UExprTranslator;
import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;
import sjtu.ipads.wtune.sqlparser.schema.Schema;

public class ProverSupport {
  public static UExpr translateAsUExpr(PlanNode plan) {
    return UExprTranslator.translate(plan);
  }

  public static Disjunction normalizeExpr(UExpr uExpr) {
    return Normalization.normalize(uExpr);
  }

  public static Disjunction canonizeExpr(Disjunction disjunction, Schema schema) {
    return Canonization.canonize(disjunction, schema);
  }

  public static LogicCtx mkLogicCtx() {
    return LogicCtx.z3();
  }

  public static LogicProver mkProver(Schema schema) {
    return LogicProver.mk(schema, LogicCtx.z3());
  }

  public static LogicProver mkProver(Schema schema, LogicCtx logicCtx) {
    return LogicProver.mk(schema, logicCtx);
  }
}
