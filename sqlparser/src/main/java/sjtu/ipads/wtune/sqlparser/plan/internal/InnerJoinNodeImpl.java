package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDef;
import sjtu.ipads.wtune.sqlparser.plan.InnerJoinNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.Commons.listJoin;

public class InnerJoinNodeImpl extends JoinNodeBase implements InnerJoinNode {
  protected InnerJoinNodeImpl(
      ASTNode onCondition,
      List<AttributeDef> used,
      List<AttributeDef> left,
      List<AttributeDef> right) {
    super(onCondition, used, left, right);
  }

  public static InnerJoinNode build(ASTNode onCondition) {
    return new InnerJoinNodeImpl(onCondition, null, null, null);
  }

  public static InnerJoinNode build(List<AttributeDef> left, List<AttributeDef> right) {
    return new InnerJoinNodeImpl(null, listJoin(left, right), left, right);
  }

  @Override
  protected PlanNode copy0() {
    return new InnerJoinNodeImpl(onCondition, used, left, right);
  }
}
