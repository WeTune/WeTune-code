package sjtu.ipads.wtune.symsolver.core;

public interface TableEq<T> extends Constraint {
  T tx();

  T ty();
}
