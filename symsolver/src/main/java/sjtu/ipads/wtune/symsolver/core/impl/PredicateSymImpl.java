package sjtu.ipads.wtune.symsolver.core.impl;

import sjtu.ipads.wtune.symsolver.core.PredicateSym;

public class PredicateSymImpl extends BaseSym implements PredicateSym {
  public static PredicateSym build() {
    return new PredicateSymImpl();
  }

  @Override
  public String toString() {
    return "p" + index();
  }
}
