package sjtu.ipads.wtune.sql.plan;

import sjtu.ipads.wtune.sql.ast.constants.SetOpKind;

public interface SetOpNode extends PlanNode {
  boolean deduplicated();

  SetOpKind opKind();

  @Override
  default PlanKind kind() {
    return PlanKind.SetOp;
  }

  static SetOpNode mk(boolean deduplicated, SetOpKind opKind) {
    return new SetOpNodeImpl(deduplicated, opKind);
  }
}
