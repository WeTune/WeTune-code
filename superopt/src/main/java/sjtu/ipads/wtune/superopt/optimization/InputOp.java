package sjtu.ipads.wtune.superopt.optimization;

import sjtu.ipads.wtune.sqlparser.rel.Relation;
import sjtu.ipads.wtune.superopt.plan.OperatorType;
import sjtu.ipads.wtune.superopt.optimization.internal.InputOpImpl;

public interface InputOp extends Operator {
  @Override
  default OperatorType type() {
    return OperatorType.Input;
  }

  Relation table();

  static InputOp make(Relation table) {
    return InputOpImpl.build(table);
  }
}
