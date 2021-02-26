package sjtu.ipads.wtune.symsolver.core;

public interface EqConstraint<T> extends Constraint {
  T left();

  T right();
}
