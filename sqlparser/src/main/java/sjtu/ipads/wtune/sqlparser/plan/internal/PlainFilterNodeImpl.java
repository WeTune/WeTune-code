package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.PlainFilterNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;

public class PlainFilterNodeImpl extends FilterNodeBase implements PlainFilterNode {
  private PlainFilterNodeImpl(ASTNode expr) {
    super(expr);
  }

  public static PlainFilterNode build(ASTNode expr) {
    return new PlainFilterNodeImpl(expr);
  }

  @Override
  public void setPredecessor(int idx, PlanNode op) {
    assert idx == 0;
  }
}
