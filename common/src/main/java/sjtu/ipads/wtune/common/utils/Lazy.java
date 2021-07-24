package sjtu.ipads.wtune.common.utils;

import java.util.function.Supplier;

public final class Lazy<T> {
  private final Supplier<T> suppiler;
  private T val;

  private Lazy(Supplier<T> suppiler) {
    this.suppiler = suppiler;
  }

  private Lazy(T val) {
    this.suppiler = null;
    this.val = val;
  }

  public static <T> Lazy<T> mk(Supplier<T> supplier) {
    return new Lazy<>(supplier);
  }

  public static <T> Lazy<T> mk(T val) {
    return new Lazy<>(val);
  }

  public final T get() {
    if (val == null) val = suppiler.get();
    return val;
  }

  public final void set(T val) {
    this.val = val;
  }
}
