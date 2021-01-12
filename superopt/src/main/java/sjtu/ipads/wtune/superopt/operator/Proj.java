package sjtu.ipads.wtune.superopt.operator;

import sjtu.ipads.wtune.superopt.internal.Placeholder;
import sjtu.ipads.wtune.superopt.operator.impl.ProjImpl;

public interface Proj extends Operator {
  Placeholder fields();

  @Override
  default OperatorType type() {
    return OperatorType.Proj;
  }

  static Proj create() {
    return ProjImpl.create();
  }
}
