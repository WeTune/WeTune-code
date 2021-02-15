package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;

import java.util.List;

public interface FilterNode extends PlanNode {
  ASTNode expr();
}
