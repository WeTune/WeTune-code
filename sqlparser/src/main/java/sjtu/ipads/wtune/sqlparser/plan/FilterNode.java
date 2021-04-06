package sjtu.ipads.wtune.sqlparser.plan;

import java.util.Collection;
import java.util.List;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.internal.PlainFilterNode;
import sjtu.ipads.wtune.sqlparser.plan.internal.SubqueryFilterNode;

public interface FilterNode extends PlanNode {
  Expr predicate();

  List<ASTNode> expr();

  Collection<AttributeDef> nonNullAttributes();

  Collection<AttributeDef> fixedValueAttributes();

  List<FilterNode> breakDown();

  static FilterNode makePlainFilter(ASTNode expr) {
    return PlainFilterNode.build(expr);
  }

  static FilterNode makePlainFilter(Expr expr, List<AttributeDef> attrs) {
    return PlainFilterNode.build(expr, attrs);
  }

  static FilterNode makeSubqueryFilter(ASTNode expr) {
    return SubqueryFilterNode.buildFromExpr(expr);
  }

  static FilterNode makeSubqueryFilter(List<AttributeDef> attrs) {
    return SubqueryFilterNode.buildFromAttributes(attrs);
  }
}
