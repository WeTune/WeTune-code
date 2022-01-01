package sjtu.ipads.wtune.superopt.substitution;

import sjtu.ipads.wtune.superopt.constraint.Constraints;
import sjtu.ipads.wtune.superopt.fragment.Symbol;

import java.util.List;

class MeaninglessChecker {
  static boolean isMeaningless(Substitution substitution) {
    return isUniform(substitution);
  }

  private static boolean isUniform(Substitution substitution) {
    // All LHS attrs symbols are required equal.
    final Constraints constraints = substitution.constraints();
    final List<Symbol> attrs = substitution._0().symbols().symbolsOf(Symbol.Kind.ATTRS);
    if (attrs.size() <= 5) return false;

    for (int i = 0; i < attrs.size() - 1; i++)
      for (int j = i + 1; j < attrs.size(); j++)
        if (!constraints.isEq(attrs.get(i), attrs.get(j))) {
          return false;
        }
    return true;
  }

  // TODO
}
