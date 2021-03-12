package sjtu.ipads.wtune.common.utils;

import java.util.function.Consumer;
import java.util.function.Function;

public interface IConsumer<T> extends Consumer<T> {
  default T apply(T t) {
    accept(t);
    return t;
  }

  default Runnable bind(T t) {
    return () -> this.accept(t);
  }
}
