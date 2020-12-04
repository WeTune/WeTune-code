package sjtu.ipads.wtune.solver.core;

import sjtu.ipads.wtune.solver.core.impl.ConstraintImpl;

public interface Constraint extends Variable {
  static Constraint wrap(Object obj) {
    return ConstraintImpl.create(obj, null);
  }

  static Constraint wrap(Object obj, String desc) {
    return ConstraintImpl.create(obj, desc);
  }
}
