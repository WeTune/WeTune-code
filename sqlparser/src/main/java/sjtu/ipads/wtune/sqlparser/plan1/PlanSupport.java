package sjtu.ipads.wtune.sqlparser.plan1;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.schema.Schema;

public interface PlanSupport {
  static PlanNode buildPlan(ASTNode ast) {
    return PlanBuilder.buildPlan(ast);
  }

  static PlanNode buildPlan(ASTNode ast, Schema schema) {
    return PlanBuilder.buildPlan(ast, schema);
  }

  static PlanNode assemblePlan(ASTNode ast) {
    final PlanNode plan = PlanBuilder.buildPlan(ast);
    RefResolver.resolve(plan);
    return plan;
  }

  static PlanNode assemblePlan(ASTNode ast, Schema schema) {
    final PlanNode plan = PlanBuilder.buildPlan(ast, schema);
    RefResolver.resolve(plan);
    return plan;
  }

  static PlanNode resolvePlan(PlanNode plan) {
    RefResolver.resolve(plan);
    return plan;
  }

  static PlanNode disambiguate(PlanNode plan) {
    Disambiguation.disambiguate(plan);
    return plan;
  }

  static ASTNode buildAst(PlanNode plan) {
    return AstBuilder.build(plan);
  }

  static boolean isDependentRef(Ref ref, PlanContext ctx) {
    final Value v = ctx.deRef(ref);
    if (v == null) throw new IllegalArgumentException("cannot resolve ref " + ref);

    final PlanNode vOwner = ctx.ownerOf(v);
    final PlanNode rOwner = ctx.ownerOf(ref);

    PlanNode path = vOwner;
    while (path != null) {
      if (path == rOwner) return false;
      path = path.successor();
    }

    return true;
  }
}
