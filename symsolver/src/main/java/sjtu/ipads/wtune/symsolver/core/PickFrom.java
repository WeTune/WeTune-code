package sjtu.ipads.wtune.symsolver.core;

public interface PickFrom<T extends Indexed, P extends Indexed> extends Constraint {
  P p();

  T[] ts();
}
