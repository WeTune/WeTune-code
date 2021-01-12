package sjtu.ipads.wtune.superopt.operator;

import sjtu.ipads.wtune.superopt.internal.Placeholder;
import sjtu.ipads.wtune.superopt.operator.impl.SubqueryFilterImpl;

public interface SubqueryFilter extends Operator {
  Placeholder fields();

  @Override
  default OperatorType type() {
    return OperatorType.SubqueryFilter;
  }

  static SubqueryFilter create() {
    return SubqueryFilterImpl.create();
  }
}
