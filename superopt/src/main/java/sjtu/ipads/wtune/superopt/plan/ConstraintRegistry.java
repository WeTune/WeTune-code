package sjtu.ipads.wtune.superopt.plan;

import sjtu.ipads.wtune.superopt.plan.symbolic.Placeholder;

public interface ConstraintRegistry {
  Placeholder[] sourceOf(Placeholder pick);
}
