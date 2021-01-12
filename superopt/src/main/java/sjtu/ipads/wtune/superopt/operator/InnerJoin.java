package sjtu.ipads.wtune.superopt.operator;

import sjtu.ipads.wtune.superopt.operator.impl.InnerJoinImpl;

public interface InnerJoin extends Join {

  @Override
  default OperatorType type() {
    return OperatorType.InnerJoin;
  }

  static InnerJoin create(){
    return InnerJoinImpl.create();
  }
}
