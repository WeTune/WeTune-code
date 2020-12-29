package sjtu.ipads.wtune.symsolver.utils;

public class SimpleTimer implements AutoCloseable {
  private final long start = System.currentTimeMillis();

  @Override
  public void close() {
    System.out.println(System.currentTimeMillis() - start);
  }
}
