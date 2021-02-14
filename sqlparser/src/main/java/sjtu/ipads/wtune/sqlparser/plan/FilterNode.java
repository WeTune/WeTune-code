package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;

public interface FilterNode extends PlanNode {
  ASTNode expr();
}
