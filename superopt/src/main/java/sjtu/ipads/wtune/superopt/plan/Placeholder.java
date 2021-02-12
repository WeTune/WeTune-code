package sjtu.ipads.wtune.superopt.plan;

import sjtu.ipads.wtune.symsolver.core.Indexed;

public interface Placeholder extends Indexed {
  // By design, placeholder is distinguished by its identity,
  // i.e. each placeholder is distinct. The index serves for
  // de/serialization and readability.
  String tag();

  PlanNode owner();
}
