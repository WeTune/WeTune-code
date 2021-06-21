package sjtu.ipads.wtune.sqlparser.plan1;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.schema.Schema;

public class PlanSupport {
  public static PlanNode buildPlan(ASTNode ast) {
    return PlanBuilder.buildPlan(ast);
  }

  public static PlanNode buildPlan(ASTNode ast, Schema schema) {
    return PlanBuilder.buildPlan(ast, schema);
  }

  public static PlanNode assemblePlan(ASTNode ast) {
    final PlanNode plan = PlanBuilder.buildPlan(ast);
    RefResolver.resolve(plan);
    return plan;
  }

  public static PlanNode assemblePlan(ASTNode ast, Schema schema) {
    final PlanNode plan = PlanBuilder.buildPlan(ast, schema);
    RefResolver.resolve(plan);
    return plan;
  }

  public static PlanNode resolvePlan(PlanNode plan) {
    RefResolver.resolve(plan);
    return plan;
  }

  public static PlanNode disambiguate(PlanNode plan){
    Disambiguation.disambiguate(plan);
    return plan;
  }
}
