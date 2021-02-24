package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.InnerJoinNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanAttribute;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.Commons.listJoin;

public class InnerJoinNodeImpl extends JoinNodeBase implements InnerJoinNode {
  protected InnerJoinNodeImpl(ASTNode onCondition) {
    super(onCondition, null, null, null);
  }

  protected InnerJoinNodeImpl(List<PlanAttribute> left, List<PlanAttribute> right) {
    super(null, listJoin(left, right), left, right);
  }

  public static InnerJoinNode build(ASTNode onCondition) {
    return new InnerJoinNodeImpl(onCondition);
  }

  public static InnerJoinNode build(List<PlanAttribute> left, List<PlanAttribute> right) {
    return new InnerJoinNodeImpl(left, right);
  }

  @Override
  protected PlanNode copy0() {
    return new InnerJoinNodeImpl(left, right);
  }
}
