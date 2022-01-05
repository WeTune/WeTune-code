package sjtu.ipads.wtune.sql.plan;

import sjtu.ipads.wtune.sql.ast.constants.SetOpKind;

class SetOpNodeImpl implements SetOpNode {
  private final boolean deduplicated;
  private final SetOpKind opKind;

  SetOpNodeImpl(boolean deduplicated, SetOpKind opKind) {
    this.deduplicated = deduplicated;
    this.opKind = opKind;
  }

  @Override
  public boolean deduplicated() {
    return deduplicated;
  }

  @Override
  public SetOpKind opKind() {
    return opKind;
  }
}
