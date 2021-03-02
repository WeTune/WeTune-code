package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.internal.ProjNodeImpl;

import java.util.List;

public interface ProjNode extends PlanNode {
  List<ASTNode> selectItems();

  boolean isWildcard();

  void setWildcard(boolean flag);

  @Override
  default OperatorType type() {
    return OperatorType.Proj;
  }

  static ProjNode make(List<AttributeDef> projs) {
    return ProjNodeImpl.build(projs);
  }

  static ProjNode make(String qualification, List<ASTNode> selectItems) {
    return ProjNodeImpl.build(qualification, selectItems);
  }
}
