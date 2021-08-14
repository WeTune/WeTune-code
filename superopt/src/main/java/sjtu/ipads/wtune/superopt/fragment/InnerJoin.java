package sjtu.ipads.wtune.superopt.fragment;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.superopt.fragment.internal.InnerJoinImpl;

public interface InnerJoin extends Join {
  @Override
  default OperatorType kind() {
    return OperatorType.INNER_JOIN;
  }

  static InnerJoin create() {
    return InnerJoinImpl.create();
  }
}
