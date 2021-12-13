package sjtu.ipads.wtune.superopt.constraint;

import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.fragment.SymbolNaming;
import sjtu.ipads.wtune.superopt.substitution.Substitution;

import java.util.List;

import static sjtu.ipads.wtune.superopt.fragment.Symbol.Kind.TABLE;

public interface ConstraintSupport {
  int ENUM_FLAG_DRY_RUN = 1;
  int ENUM_FLAG_DISABLE_BREAKER_0 = 3;
  int ENUM_FLAG_DISABLE_BREAKER_1 = 5;

  static StringBuilder stringify(
      Constraint c, SymbolNaming naming, boolean canonical, StringBuilder builder) {
    return new ConstraintStringifier(naming, canonical, builder).stringify(c);
  }

  static StringBuilder stringify(
      Constraints C, SymbolNaming naming, boolean canonical, StringBuilder builder) {
    return new ConstraintStringifier(naming, canonical, builder).stringify(C);
  }

  static List<Substitution> enumConstraints(Fragment f0, Fragment f1, long timeout) {
    return enumConstraints(f0, f1, timeout, 0);
  }

  static List<Substitution> enumConstraints(Fragment f0, Fragment f1, long timeout, int tweaks) {
    final int bias = pickSource(f0, f1);
    List<Substitution> rules = null;

    if ((bias & 1) != 0) {
      final ConstraintsIndex I = new ConstraintsIndex(f0, f1);
      final ConstraintEnumerator enumerator = new ConstraintEnumerator(I, timeout, tweaks);
      rules = enumerator.enumerate();
    }

    if ((bias & 2) != 0) {
      final ConstraintsIndex I = new ConstraintsIndex(f1, f0);
      final ConstraintEnumerator enumerator = new ConstraintEnumerator(I, timeout, tweaks);
      final List<Substitution> rules2 = enumerator.enumerate();
      if (rules == null) rules = rules2;
      else rules.addAll(rules2);
    }

    return rules;
  }

  private static int pickSource(Fragment f0, Fragment f1) {
    if (f0.equals(f1)) return 1;
    final int numTables0 = f0.symbolCount(TABLE), numTables1 = f1.symbolCount(TABLE);
    if (numTables0 > numTables1) return 1;
    else if (numTables0 < numTables1) return 2;
    else return 3;
  }
}
