package sjtu.ipads.wtune.symsolver.core;

public interface TableEq<T> extends EqConstraint<T> {
  T tx();

  T ty();

  @Override
  default T left() {
    return tx();
  }

  @Override
  default T right() {
    return ty();
  }

  @Override
  default boolean involves(Object obj) {
    return tx() == obj || ty() == obj;
  }
}
