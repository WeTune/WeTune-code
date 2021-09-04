package sjtu.ipads.wtune.superopt.substitution;

import sjtu.ipads.wtune.common.utils.IgnorableException;
import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.constraint.Constraints;
import sjtu.ipads.wtune.superopt.fragment.Symbol;
import sjtu.ipads.wtune.superopt.fragment.Symbols;

import java.util.List;

import static sjtu.ipads.wtune.superopt.substitution.SubstitutionSupport.translateAsPlan;

class MeaninglessChecker {
  static boolean isMeaningless(Substitution substitution) {
    return isUniform(substitution) || isConfusing(substitution) || isIllegal(substitution);
  }

  private static boolean isIdentical(Substitution substitution) {
    return substitution._0().toString().equals(substitution._1().toString());
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

  private static boolean isConfusing(Substitution substitution) {
    // Two RHS table symbols are required equal.
    // This disturbs value-binding during optimization. See PlanSupport::bindValuesRelaxed.
    final Symbols rhs = substitution._1().symbols();
    for (Constraint tableEq : substitution.constraints().ofKind(Constraint.Kind.TableEq)) {
      if (tableEq.symbols()[0].ctx() == rhs && tableEq.symbols()[1].ctx() == rhs) return true;
    }
    return false;
  }

  private static boolean isIllegal(Substitution substitution) {
    try {
      translateAsPlan(substitution, false, false);
      return false;
    } catch (IgnorableException ex) {
      return true;
    }
  }
}
