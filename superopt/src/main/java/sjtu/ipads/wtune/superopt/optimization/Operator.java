package sjtu.ipads.wtune.superopt.optimization;

import sjtu.ipads.wtune.superopt.plan.OperatorType;

public interface Operator {
  OperatorType type();

  Operator[] predecessors();
}
