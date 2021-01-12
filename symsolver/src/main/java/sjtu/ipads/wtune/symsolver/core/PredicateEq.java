package sjtu.ipads.wtune.symsolver.core;

public interface PredicateEq<P extends Indexed> extends Constraint {
  P px();

  P py();
}
