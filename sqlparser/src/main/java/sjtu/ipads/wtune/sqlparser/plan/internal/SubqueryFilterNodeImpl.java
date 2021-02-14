package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.OutputAttribute;
import sjtu.ipads.wtune.sqlparser.plan.SubqueryFilterNode;

import java.util.List;

public class SubqueryFilterNodeImpl extends FilterNodeBase implements SubqueryFilterNode {
  private SubqueryFilterNodeImpl(ASTNode expr) {
    super(expr);
  }

  public static SubqueryFilterNode build(ASTNode expr) {
    return new SubqueryFilterNodeImpl(expr);
  }
}
