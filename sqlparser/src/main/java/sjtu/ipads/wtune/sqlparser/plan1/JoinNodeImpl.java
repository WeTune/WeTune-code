package sjtu.ipads.wtune.sqlparser.plan1;

import sjtu.ipads.wtune.sqlparser.ast1.constants.JoinKind;

import java.util.List;

class JoinNodeImpl implements JoinNode {
  private final JoinKind joinKind;
  private final Expression joinCond;

  JoinNodeImpl(JoinKind joinKind, Expression joinCond) {
    this.joinKind = joinKind;
    this.joinCond = joinCond;
  }

  @Override
  public JoinKind joinKind() {
    return joinKind;
  }

  @Override
  public Expression joinCond() {
    return joinCond;
  }
}
