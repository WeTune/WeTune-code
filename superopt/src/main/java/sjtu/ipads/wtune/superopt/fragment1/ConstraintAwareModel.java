package sjtu.ipads.wtune.superopt.fragment1;

import sjtu.ipads.wtune.sqlparser.plan1.PlanContext;
import sjtu.ipads.wtune.superopt.constraint.Constraints;

public interface ConstraintAwareModel extends Model {
  Constraints constraints();

  void reset();

  boolean checkConstraint();

  @Override
  ConstraintAwareModel derive();

  static ConstraintAwareModel mk(PlanContext plan, Constraints constraints) {
    return new ConstraintAwareModelImpl(plan, constraints);
  }
}
