package sjtu.ipads.wtune.symsolver.core.impl;

import sjtu.ipads.wtune.symsolver.core.PredicateSym;
import sjtu.ipads.wtune.symsolver.core.Scoped;

public class PredicateSymImpl extends BaseSym implements PredicateSym {
  private PredicateSymImpl(Scoped scoped) {
    super(scoped);
  }

  public static PredicateSym build(Scoped scoped) {
    return new PredicateSymImpl(scoped);
  }

  @Override
  public String toString() {
    return "p" + index();
  }
}
