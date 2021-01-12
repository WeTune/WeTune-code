package sjtu.ipads.wtune.symsolver.core;

public interface TableEq<T extends Indexed> extends Constraint {
  T tx();

  T ty();
}
