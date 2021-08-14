package sjtu.ipads.wtune.sqlparser.plan;

import java.util.List;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.internal.ProjNodeImpl;

public interface ProjNode extends PlanNode {
  List<ASTNode> selections();

  boolean isForcedUnique();

  boolean isWildcard();

  void setForcedUnique(boolean flag);

  void setWildcard(boolean flag);

  void setQualification(String qualification);

  @Override
  default OperatorType kind() {
    return OperatorType.PROJ;
  }

  static ProjNode make(List<AttributeDef> projs) {
    return ProjNodeImpl.build(projs);
  }

  static ProjNode make(String qualification, List<ASTNode> selectItems) {
    return ProjNodeImpl.build(qualification, selectItems);
  }

  static ProjNode makeWildcard(List<AttributeDef> usedAttrs) {
    return ProjNodeImpl.buildWildcard(usedAttrs);
  }
}
