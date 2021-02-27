package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.LeftJoinNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanAttribute;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;

import java.util.List;

public class LeftJoinNodeImpl extends JoinNodeBase implements LeftJoinNode {
  protected LeftJoinNodeImpl(ASTNode onCondition) {
    super(onCondition, null, null, null);
  }

  protected LeftJoinNodeImpl(
      ASTNode onCondition,
      List<PlanAttribute> used,
      List<PlanAttribute> left,
      List<PlanAttribute> right) {
    super(onCondition, used, left, right);
  }

  public static LeftJoinNode build(ASTNode onCondition) {
    return new LeftJoinNodeImpl(onCondition, null, null, null);
  }

  public static LeftJoinNode build(List<PlanAttribute> left, List<PlanAttribute> right) {
    return new LeftJoinNodeImpl(null, left, right, null);
  }

  @Override
  protected PlanNode copy0() {
    return new LeftJoinNodeImpl(onCondition, used, left, right);
  }
}
