package sjtu.ipads.wtune.superopt.plan.symbolic;

import sjtu.ipads.wtune.superopt.util.PlaceholderNumbering;
import sjtu.ipads.wtune.symsolver.core.Indexed;

public interface Placeholder extends Indexed {
  String tag();

  Placeholder copy();

  static PlaceholderNumbering numbering() {
    return PlaceholderNumbering.build();
  }
}
