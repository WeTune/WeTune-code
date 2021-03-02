package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.PlainFilterNode;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDef;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;

import java.util.List;

public class PlainFilterNodeImpl extends FilterNodeBase implements PlainFilterNode {
  private PlainFilterNodeImpl(ASTNode expr, List<AttributeDef> usedAttrs) {
    super(expr, usedAttrs);
  }

  public static PlainFilterNode build(ASTNode expr) {
    return new PlainFilterNodeImpl(expr, null);
  }

  public static PlainFilterNode build(ASTNode expr, List<AttributeDef> usedAttrs) {
    return new PlainFilterNodeImpl(expr, usedAttrs);
  }

  @Override
  protected PlanNode copy0() {
    return new PlainFilterNodeImpl(expr, usedAttrs);
  }
}
