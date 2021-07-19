package sjtu.ipads.wtune.common.utils;

import java.util.function.Supplier;

public final class Lazy<T> {
  private final Supplier<T> suppiler;
  private T val;

  private Lazy(Supplier<T> suppiler) {
    this.suppiler = suppiler;
  }

  public static <T> Lazy<T> mk(Supplier<T> supplier) {
    return new Lazy<>(supplier);
  }

  public final T get() {
    if (val == null) val = suppiler.get();
    return val;
  }
}
