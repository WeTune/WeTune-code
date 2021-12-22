package sjtu.ipads.wtune.sqlparser.plan1;

import sjtu.ipads.wtune.sqlparser.ast1.constants.JoinKind;

import java.util.List;

public interface JoinNode extends PlanNode {
  JoinKind joinKind();

  Expression joinCond();

  boolean isEquiJoin();

  List<Value> lhsKeys();

  List<Value> rhsKeys();

  void setKeys(List<Value> lhsKeys, List<Value> rhsKeys);

  @Override
  default PlanKind kind() {
    return PlanKind.Join;
  }

  static JoinNode mk(JoinKind kind, Expression joinCond) {
    return new JoinNodeImpl(kind, joinCond);
  }
}
