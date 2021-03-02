package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDef;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan.SubqueryFilterNode;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.TUPLE_EXPRS;

public class SubqueryFilterNodeImpl extends FilterNodeBase implements SubqueryFilterNode {
  private SubqueryFilterNodeImpl(ASTNode expr) {
    super(expr, null);
  }

  private SubqueryFilterNodeImpl(ASTNode expr, List<AttributeDef> usedAttrs) {
    super(expr, usedAttrs);
  }

  public static SubqueryFilterNode build(ASTNode expr) {
    return new SubqueryFilterNodeImpl(expr);
  }

  public static SubqueryFilterNode build(List<AttributeDef> usedAttrs) {
    return new SubqueryFilterNodeImpl(makeLeftExpr(usedAttrs), usedAttrs);
  }

  @Override
  public ASTNode leftExpr() {
    return makeLeftExpr(usedAttrs);
  }

  @Override
  protected PlanNode copy0() {
    return new SubqueryFilterNodeImpl(expr, usedAttrs);
  }

  private static ASTNode makeLeftExpr(List<AttributeDef> usedAttrs) {
    if (usedAttrs.size() == 1) return usedAttrs.get(0).toColumnRef();
    else {
      final List<ASTNode> colRefs = listMap(AttributeDef::toColumnRef, usedAttrs);
      final ASTNode tuple = ASTNode.expr(ExprKind.TUPLE);
      tuple.set(TUPLE_EXPRS, colRefs);
      return tuple;
    }
  }
}
