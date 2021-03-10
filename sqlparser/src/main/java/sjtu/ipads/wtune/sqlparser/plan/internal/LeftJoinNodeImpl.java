package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDef;
import sjtu.ipads.wtune.sqlparser.plan.LeftJoinNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.Commons.listJoin;

public class LeftJoinNodeImpl extends JoinNodeBase implements LeftJoinNode {
  protected LeftJoinNodeImpl(
      ASTNode onCondition,
      List<AttributeDef> used,
      List<AttributeDef> left,
      List<AttributeDef> right) {
    super(onCondition, used, left, right);
  }

  public static LeftJoinNode build(ASTNode onCondition) {
    return new LeftJoinNodeImpl(onCondition, null, null, null);
  }

  public static LeftJoinNode build(List<AttributeDef> left, List<AttributeDef> right) {
    return new LeftJoinNodeImpl(null, listJoin(left, right), left, right);
  }

  @Override
  protected PlanNode copy0() {
    return new LeftJoinNodeImpl(onCondition, used, left, right);
  }
}
