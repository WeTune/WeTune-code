package sjtu.ipads.wtune.common.utils;

import java.util.function.Function;
import java.util.function.Predicate;

@FunctionalInterface
public interface IPredicate<T> extends Predicate<T>, Function<T, Boolean> {
  @Override
  default boolean test(T t) {
    return apply(t);
  }
}
