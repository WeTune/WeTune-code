package sjtu.ipads.wtune.symsolver.core;

public interface TableEq<T> extends Constraint {
  T tx();

  T ty();

  @Override
  default boolean involves(Object obj) {
    return tx() == obj || ty() == obj;
  }
}
