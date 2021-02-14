package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.internal.ProjNodeImpl;
import sjtu.ipads.wtune.sqlparser.relational.Relation;

import java.util.List;

public interface ProjNode extends PlanNode {

  List<ASTNode> selectItems();

  @Override
  default OperatorType type() {
    return OperatorType.Proj;
  }

  static ProjNode make(Relation relation) {
    return ProjNodeImpl.build(relation);
  }
}
