package sjtu.ipads.wtune.symsolver.core;

public interface PickEq<P extends Indexed> extends Constraint {
  P px();

  P py();
}