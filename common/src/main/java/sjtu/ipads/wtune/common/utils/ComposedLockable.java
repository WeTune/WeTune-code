package sjtu.ipads.wtune.common.utils;

public class ComposedLockable implements Lockable {
  private final Lockable[] lockables;

  public ComposedLockable(Lockable[] lockables) {
    this.lockables = lockables;
  }

  @Override
  public void lock() {
    for (Lockable lockable : lockables) lockable.lock();
  }

  @Override
  public void unlock() {
    for (Lockable lockable : lockables) lockable.unlock();
  }
}
