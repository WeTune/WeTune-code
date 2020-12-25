package sjtu.ipads.wtune.symsolver.core;

public interface Prover extends Reactor {
  void prepare(Decision[] choices);

  boolean prove();
}
