package sjtu.ipads.wtune.superopt.constraint;

import sjtu.ipads.wtune.common.utils.Lazy;
import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.fragment.SymbolNaming;
import sjtu.ipads.wtune.superopt.substitution.Substitution;
import sjtu.ipads.wtune.superopt.util.Complexity;

import java.util.List;

import static java.util.Collections.emptyList;
import static sjtu.ipads.wtune.superopt.fragment.Symbol.Kind.*;

public interface ConstraintSupport {
  int ENUM_FLAG_DRY_RUN = 1;
  int ENUM_FLAG_DISABLE_BREAKER_0 = 2 | ENUM_FLAG_DRY_RUN;
  int ENUM_FLAG_DISABLE_BREAKER_1 = 4 | ENUM_FLAG_DRY_RUN;
  int ENUM_FLAG_DISABLE_BREAKER_2 = 8 | ENUM_FLAG_DRY_RUN;
  int ENUM_FLAG_ECHO = 16;
  int ENUM_FLAG_USE_SPES = 32;

  static EnumerationMetrics getMetrics() {
    return EnumerationMetricsContext.instance().global();
  }

  static StringBuilder stringify(
      Constraint c, SymbolNaming naming, boolean canonical, StringBuilder builder) {
    return new ConstraintStringifier(naming, canonical, builder).stringify(c);
  }

  static StringBuilder stringify(
      Constraints C, SymbolNaming naming, boolean canonical, StringBuilder builder) {
    return new ConstraintStringifier(naming, canonical, builder).stringify(C);
  }

  static List<Substitution> enumConstraints(Fragment f0, Fragment f1, long timeout) {
    return enumConstraints(f0, f1, timeout, 0, null);
  }

  static List<Substitution> enumConstraints(
      Fragment f0, Fragment f1, long timeout, int tweaks, SymbolNaming naming) {
    final int bias = pickSource(f0, f1);
    if (bias == 0) return emptyList();

    List<Substitution> rules = null;

    if ((bias & 1) != 0) {
      final ConstraintsIndex I = new ConstraintsIndex(f0, f1);
      final ConstraintEnumerator enumerator = new ConstraintEnumerator(I, timeout, tweaks);
      enumerator.setNaming(naming);
      rules = enumerator.enumerate();
    }

    if ((bias & 2) != 0) {
      final ConstraintsIndex I = new ConstraintsIndex(f1, f0);
      final ConstraintEnumerator enumerator = new ConstraintEnumerator(I, timeout, tweaks);
      enumerator.setNaming(naming);

      final List<Substitution> rules2 = enumerator.enumerate();
      if (rules == null) rules = rules2;
      else rules.addAll(rules2);
    }

    return rules;
  }

  static List<Substitution> enumConstraints2(
      Fragment f0, Fragment f1, long timeout, int tweaks, SymbolNaming naming) {
    final int bias = pickSource(f0, f1);
    if (bias == 0) return emptyList();

    List<Substitution> rules = null;

    if ((bias & 1) != 0) {
      final ConstraintsIndex2 I = new ConstraintsIndex2(f0, f1);
      final ConstraintEnumerator2 enumerator = new ConstraintEnumerator2(I, timeout, tweaks);
      enumerator.setNaming(naming);
      rules = enumerator.enumerate();
    }

    if ((bias & 2) != 0) {
      final ConstraintsIndex2 I = new ConstraintsIndex2(f1, f0);
      final ConstraintEnumerator2 enumerator = new ConstraintEnumerator2(I, timeout, tweaks);
      enumerator.setNaming(naming);

      final List<Substitution> rules2 = enumerator.enumerate();
      if (rules == null) rules = rules2;
      else rules.addAll(rules2);
    }

    return rules;
  }

  private static int pickSource(Fragment f0, Fragment f1) {
    if (f0.equals(f1)) return 1;

    final int numTables0 = f0.symbolCount(TABLE), numTables1 = f1.symbolCount(TABLE);
    final int numAttrs0 = f0.symbolCount(ATTRS), numAttrs1 = f1.symbolCount(ATTRS);
    final int numPreds0 = f0.symbolCount(PRED), numPreds1 = f1.symbolCount(PRED);
    final Lazy<Complexity> complexity0 = Lazy.mk(() -> Complexity.mk(f0));
    final Lazy<Complexity> complexity1 = Lazy.mk(() -> Complexity.mk(f1));

    int qualified = 0;

    if (numTables0 >= numTables1
        && (numAttrs0 != 0 || numAttrs1 == 0)
        && (numPreds0 != 0 || numPreds1 == 0)
        && complexity0.get().compareTo(complexity1.get()) >= 0) qualified |= 1;

    if (numTables1 >= numTables0
        && (numAttrs1 != 0 || numAttrs0 == 0)
        && (numPreds1 != 0 || numPreds0 == 0)
        && complexity1.get().compareTo(complexity0.get()) >= 0) qualified |= 2;

    return qualified;
  }
}
