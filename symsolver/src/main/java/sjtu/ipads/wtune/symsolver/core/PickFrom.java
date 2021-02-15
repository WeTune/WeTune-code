package sjtu.ipads.wtune.symsolver.core;

public interface PickFrom<T, P> extends Constraint {
  P p();

  T[] ts();

  @Override
  default boolean involves(Object obj) {
    if (p() == obj) return true;
    for (T t : ts()) if (t == obj) return true;
    return false;
  }
}
