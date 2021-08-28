package sjtu.ipads.wtune.superopt.substitution;

import sjtu.ipads.wtune.superopt.constraint.Constraints;
import sjtu.ipads.wtune.superopt.fragment1.Symbol;

import java.util.List;

class MeaninglessChecker {
  static boolean isMeaningless(Substitution substitution) {
    return isIdentical(substitution) || isUniform(substitution);
  }

  private static boolean isIdentical(Substitution substitution) {
    return substitution._0().toString().equals(substitution._1().toString());
  }

  private static boolean isUniform(Substitution substitution) {
    final Constraints constraints = substitution.constraints();
    final List<Symbol> attrs = substitution._0().symbols().symbolsOf(Symbol.Kind.ATTRS);
    for (int i = 0; i < attrs.size() - 1; i++)
      for (int j = i + 1; j < attrs.size(); j++)
        if (!constraints.isEq(attrs.get(i), attrs.get(j))) {
          return false;
        }
    return true;
  }
}
