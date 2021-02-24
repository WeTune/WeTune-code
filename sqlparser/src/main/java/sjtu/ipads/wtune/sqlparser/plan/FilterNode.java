package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.relational.Attribute;

import java.util.List;

public interface FilterNode extends PlanNode {
  ASTNode expr();
}
