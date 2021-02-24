package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.LeftJoinNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanAttribute;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.Commons.listJoin;

public class LeftJoinNodeImpl extends JoinNodeBase implements LeftJoinNode {
  protected LeftJoinNodeImpl(ASTNode onCondition) {
    super(onCondition, null, null, null);
  }

  protected LeftJoinNodeImpl(List<PlanAttribute> left, List<PlanAttribute> right) {
    super(null, listJoin(left, right), left, right);
  }

  public static LeftJoinNode build(ASTNode onCondition) {
    return new LeftJoinNodeImpl(onCondition);
  }

  public static LeftJoinNode build(List<PlanAttribute> left, List<PlanAttribute> right) {
    return new LeftJoinNodeImpl(left, right);
  }

  @Override
  protected PlanNode copy0() {
    return new LeftJoinNodeImpl(left, right);
  }
}
