package sjtu.ipads.wtune.symsolver.core;

public interface Reference<T, P> extends Constraint {
  T tx();

  P px();

  T ty();

  P py();

  @Override
  default boolean involves(Object obj) {
    return px() == obj || py() == obj || tx() == obj || ty() == obj;
  }
}
