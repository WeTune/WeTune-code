package sjtu.ipads.wtune.symsolver.core;

public interface PredicateEq<P> extends Constraint {
  P px();

  P py();

  @Override
  default boolean involves(Object obj) {
    return px() == obj || py() == obj;
  }
}
