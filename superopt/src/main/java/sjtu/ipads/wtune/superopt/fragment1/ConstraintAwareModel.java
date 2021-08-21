package sjtu.ipads.wtune.superopt.fragment1;

import sjtu.ipads.wtune.superopt.constraint.Constraints;

public interface ConstraintAwareModel extends Model {
  Constraints constraints();

  void reset();

  boolean checkConstraint();
}
