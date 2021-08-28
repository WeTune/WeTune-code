package sjtu.ipads.wtune.superopt.fragment;

import sjtu.ipads.wtune.sqlparser.plan.PlanContext;
import sjtu.ipads.wtune.superopt.constraint.Constraints;

public interface ConstraintAwareModel extends Model {
  Constraints constraints();

  void reset();

  boolean checkConstraint();

  @Override
  ConstraintAwareModel base();

  @Override
  ConstraintAwareModel derive();

  static ConstraintAwareModel mk(PlanContext plan, Constraints constraints) {
    return new ConstraintAwareModelImpl(plan, constraints);
  }
}
