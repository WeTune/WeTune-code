package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.internal.SortNodeImpl;

import java.util.List;

public interface SortNode extends PlanNode {
  List<ASTNode> orderKeys();

  @Override
  default OperatorType type() {
    return OperatorType.Sort;
  }

  static SortNode make(List<ASTNode> orderKeys) {
    return SortNodeImpl.build(orderKeys);
  }
}
