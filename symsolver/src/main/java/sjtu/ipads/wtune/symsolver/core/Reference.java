package sjtu.ipads.wtune.symsolver.core;

public interface Reference<T extends Indexed, P extends Indexed> extends Constraint {
  T tx();

  P px();

  T ty();

  P py();
}
