package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind;
import sjtu.ipads.wtune.sqlparser.plan.PlanAttribute;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan.SubqueryFilterNode;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.TUPLE_EXPRS;

public class SubqueryFilterNodeImpl extends FilterNodeBase implements SubqueryFilterNode {
  private SubqueryFilterNodeImpl(ASTNode expr) {
    super(expr, null);
  }

  private SubqueryFilterNodeImpl(ASTNode expr, List<PlanAttribute> usedAttrs) {
    super(expr, usedAttrs);
  }

  public static SubqueryFilterNode build(ASTNode expr) {
    return new SubqueryFilterNodeImpl(expr);
  }

  public static SubqueryFilterNode build(List<PlanAttribute> usedAttrs) {
    final List<ASTNode> columnRefs = listMap(PlanAttribute::toColumnRef, usedAttrs);
    if (columnRefs.size() == 1) return new SubqueryFilterNodeImpl(columnRefs.get(0), usedAttrs);
    else {
      final ASTNode tuple = ASTNode.expr(ExprKind.TUPLE);
      tuple.set(TUPLE_EXPRS, columnRefs);
      return new SubqueryFilterNodeImpl(tuple, usedAttrs);
    }
  }

  @Override
  protected PlanNode copy0() {
    return new SubqueryFilterNodeImpl(expr, usedAttrs);
  }
}
