package sjtu.ipads.wtune.symsolver.core;

public interface PickSub<P> extends Constraint {
  P px();

  P py();

  @Override
  default Kind kind() {
    return Kind.PickSub;
  }

  @Override
  default boolean involves(Object obj) {
    return px() == obj || py() == obj;
  }
}
