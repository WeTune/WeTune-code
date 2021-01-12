package sjtu.ipads.wtune.superopt.operator;

import sjtu.ipads.wtune.superopt.internal.Placeholder;
import sjtu.ipads.wtune.superopt.operator.impl.InputImpl;

public interface Input extends Operator {
  static Input create() {
    return InputImpl.create();
  }

  int index();

  void setIndex(int index);

  Placeholder table();

  @Override
  default OperatorType type() {
    return OperatorType.Input;
  }
}
