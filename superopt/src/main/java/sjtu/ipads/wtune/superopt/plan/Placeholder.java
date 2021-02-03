package sjtu.ipads.wtune.superopt.plan;

import sjtu.ipads.wtune.superopt.util.NumberPlaceholders;
import sjtu.ipads.wtune.symsolver.core.Indexed;
import sjtu.ipads.wtune.symsolver.core.Scoped;

public interface Placeholder extends Indexed, Scoped {
  String tag();

  Placeholder copy();

  static NumberPlaceholders numbering() {
    return NumberPlaceholders.build(true);
  }

  static NumberPlaceholders numbering(boolean withCollecting) {
    return NumberPlaceholders.build(withCollecting);
  }
}
