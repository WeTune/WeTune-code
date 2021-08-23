package sjtu.ipads.wtune.superopt.substitution;

class MeaninglessChecker {
  static boolean isMeaningless(Substitution substitution) {
    return substitution._0().toString().equals(substitution._1().toString());
  }
}
