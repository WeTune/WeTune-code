package sjtu.ipads.wtune.symsolver.core;

public interface PickFrom<T, P> extends Constraint {
  P p();

  T[] ts();
}
