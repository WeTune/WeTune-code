package sjtu.ipads.wtune.sqlparser.plan1;

import sjtu.ipads.wtune.sqlparser.ast1.constants.JoinKind;

import java.util.List;

class JoinNodeImpl implements JoinNode {
  private final JoinKind joinKind;
  private final Expression joinCond;
  private List<Value> lhsKeys, rhsKeys;

  JoinNodeImpl(JoinKind joinKind, Expression joinCond) {
    this.joinKind = joinKind;
    this.joinCond = joinCond;
  }

  @Override
  public boolean isEquiJoin() {
    return lhsKeys != null;
  }

  @Override
  public List<Value> lhsKeys() {
    return lhsKeys;
  }

  @Override
  public List<Value> rhsKeys() {
    return rhsKeys;
  }

  @Override
  public void setKeys(List<Value> lhsKeys, List<Value> rhsKeys) {
    if (lhsKeys == null || rhsKeys == null || lhsKeys.isEmpty() || lhsKeys.size() != rhsKeys.size())
      throw new IllegalArgumentException("invalid join key");

    this.lhsKeys = lhsKeys;
    this.rhsKeys = rhsKeys;
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
