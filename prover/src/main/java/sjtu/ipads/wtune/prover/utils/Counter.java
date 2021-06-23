package sjtu.ipads.wtune.prover.utils;

public class Counter {
  private int next = 0;

  public int next() {
    return next++;
  }
}
