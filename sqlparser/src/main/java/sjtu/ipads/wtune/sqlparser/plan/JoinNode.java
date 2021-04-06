package sjtu.ipads.wtune.sqlparser.plan;

import java.util.List;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.internal.JoinNodeImpl;

public interface JoinNode extends PlanNode {
  ASTNode onCondition();

  boolean isNormalForm();

  List<AttributeDef> leftAttributes();

  List<AttributeDef> rightAttributes();

  void setJoinType(OperatorType type);

  static JoinNode make(OperatorType type, ASTNode onCondition) {
    return JoinNodeImpl.build(type, onCondition);
  }

  static JoinNode make(OperatorType type, List<AttributeDef> left, List<AttributeDef> right) {
    return JoinNodeImpl.build(type, left, right);
  }
}
