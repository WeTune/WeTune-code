package sjtu.ipads.wtune.superopt.fragment;

import sjtu.ipads.wtune.sqlparser.plan.PlanContext;
import sjtu.ipads.wtune.superopt.constraint.Constraints;

public interface ConstraintAwareModel extends Model {
  Constraints constraints();

  void reset();

  boolean checkConstraint(boolean strict);

  @Override
  ConstraintAwareModel base();

  @Override
  ConstraintAwareModel derive();

  ConstraintAwareModel derive(PlanContext ctx);

  static ConstraintAwareModel mk(PlanContext plan, Constraints constraints) {
    return new ConstraintAwareModelImpl(plan, constraints);
  }
}
