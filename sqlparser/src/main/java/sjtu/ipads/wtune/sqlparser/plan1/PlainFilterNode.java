package sjtu.ipads.wtune.sqlparser.plan1;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;

public interface PlainFilterNode extends FilterNode {
  @Override
  default OperatorType type() {
    return OperatorType.PlainFilter;
  }
}
