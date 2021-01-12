package sjtu.ipads.wtune.superopt.operator;

import sjtu.ipads.wtune.superopt.internal.Placeholder;
import sjtu.ipads.wtune.superopt.operator.impl.PlainFilterImpl;

public interface PlainFilter extends Operator {
  Placeholder fields();

  Placeholder predicate();

  @Override
  default OperatorType type() {
    return OperatorType.PlainFilter;
  }

  static PlainFilter create() {
    return PlainFilterImpl.create();
  }
}
