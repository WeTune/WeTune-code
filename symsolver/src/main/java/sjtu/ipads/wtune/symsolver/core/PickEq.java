package sjtu.ipads.wtune.symsolver.core;

public interface PickEq<P> extends EqConstraint<P> {
  P px();

  P py();

  @Override
  default P left() {
    return px();
  }

  @Override
  default P right() {
    return py();
  }

  @Override
  default boolean involves(Object obj) {
    return px() == obj || py() == obj;
  }
}
