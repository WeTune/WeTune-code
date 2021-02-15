package sjtu.ipads.wtune.symsolver.core;

public interface PickEq<P> extends Constraint {
  P px();

  P py();

  @Override
  default boolean involves(Object obj) {
    return px() == obj || py() == obj;
  }
}
