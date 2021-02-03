package sjtu.ipads.wtune.symsolver.core;

public interface PredicateEq<P> extends Constraint {
  P px();

  P py();
}
