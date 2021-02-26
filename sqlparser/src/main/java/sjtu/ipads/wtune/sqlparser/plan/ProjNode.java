package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.internal.ProjNodeImpl;

import java.util.List;

public interface ProjNode extends PlanNode {
  List<ASTNode> selectItems();

  @Override
  default OperatorType type() {
    return OperatorType.Proj;
  }

  static ProjNode make(List<PlanAttribute> projs) {
    return ProjNodeImpl.build(projs);
  }

  static ProjNode copyFrom(List<PlanAttribute> defined, List<PlanAttribute> used) {
    return ProjNodeImpl.build(defined, used);
  }
}
