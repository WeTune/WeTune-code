package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;

import java.util.List;

public interface JoinNode extends PlanNode {
  ASTNode onCondition();

  boolean isNormalForm();

  List<AttributeDef> leftAttributes();

  List<AttributeDef> rightAttributes();

  JoinNode toInnerJoin();

  JoinNode toLeftJoin();
}
