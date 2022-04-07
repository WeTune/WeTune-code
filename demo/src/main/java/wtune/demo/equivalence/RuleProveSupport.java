package wtune.demo.equivalence;

import wtune.superopt.substitution.Substitution;

public class RuleProveSupport {
  public static final int BUILT_IN_PROVER_TWEAK = 1;
  public static final int SPES_PROVER_TWEAK = 2;


  /**
   * Prove whether a plan template pair is EQ under a set of constraints.
   * That is, prove a candidate rule is correct.
   */
  public static boolean isEquivalentTemplate(Substitution rule, int tweaks) {
    //if (rule == null) return false;
    //
    //boolean res;
    //if ((tweaks & BUILT_IN_PROVER_TWEAK) != 0) {
    //
    //}
    return false;
  }
}
