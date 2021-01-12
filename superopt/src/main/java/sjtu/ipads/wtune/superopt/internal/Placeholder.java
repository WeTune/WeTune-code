package sjtu.ipads.wtune.superopt.internal;

import sjtu.ipads.wtune.superopt.core.Graph;
import sjtu.ipads.wtune.superopt.util.NumberPlaceholders;
import sjtu.ipads.wtune.symsolver.core.Indexed;
import sjtu.ipads.wtune.symsolver.core.Scoped;

import java.util.Map;

public interface Placeholder extends Indexed, Scoped {
  String tag();

  static Map<String, Placeholder> numberPlaceholder(Graph... graphs) {
    return NumberPlaceholders.number(graphs);
  }
}
