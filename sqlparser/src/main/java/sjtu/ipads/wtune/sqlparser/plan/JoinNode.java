package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;

import java.util.List;

public interface JoinNode extends PlanNode {
  ASTNode onCondition();

  boolean isNormalForm();

  List<OutputAttribute> leftAttributes();

  List<OutputAttribute> rightAttributes();

  List<OutputAttribute> usedAttributes();
}
