package sjtu.ipads.wtune.common.utils;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface IFunction<T, R> extends Function<T, R> {
  default Consumer<T> then(Consumer<R> consumer) {
    return t -> consumer.accept(apply(t));
  }

  default ISupplier<R> bind(T t) {
    return () -> apply(t);
  }
}
