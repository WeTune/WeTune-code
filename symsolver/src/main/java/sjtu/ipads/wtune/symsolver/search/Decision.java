package sjtu.ipads.wtune.symsolver.search;

public interface Decision {
  void decide(Reactor reactor);

  default boolean ignorable() {
    return false;
  }
}
