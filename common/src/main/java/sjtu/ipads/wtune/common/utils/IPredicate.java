package sjtu.ipads.wtune.common.utils;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

@FunctionalInterface
public interface IPredicate<T> extends Predicate<T>, Function<T, Boolean> {
  @Override
  default boolean test(T t) {
    return apply(t);
  }

  @Override
  default IPredicate<T> and(Predicate<? super T> other) {
    Objects.requireNonNull(other);
    return t -> test(t) && other.test(t);
  }

  @Override
  default IPredicate<T> or(Predicate<? super T> other) {
    Objects.requireNonNull(other);
    return t -> test(t) || other.test(t);
  }
}
