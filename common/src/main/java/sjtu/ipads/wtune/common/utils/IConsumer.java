package sjtu.ipads.wtune.common.utils;

import java.util.function.Consumer;

public interface IConsumer<T> extends Consumer<T> {
  default Runnable bind(T t) {
    return () -> this.accept(t);
  }
}
