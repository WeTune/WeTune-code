package sjtu.ipads.wtune.prover.utils;

public class Counter {
  private int counter = 0;

  public final int addAssign() {
    return counter++;
  }

  public final int assignAdd() {
    return ++counter;
  }

  public final int get() {
    return counter;
  }
}
