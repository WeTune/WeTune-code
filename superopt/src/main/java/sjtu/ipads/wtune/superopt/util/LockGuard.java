package sjtu.ipads.wtune.superopt.util;

public class LockGuard implements AutoCloseable {
  private final Lockable l;

  public LockGuard(Lockable l) {
    this.l = l;
    l.lock();
  }

  @Override
  public void close() {
    l.unlock();
  }
}
