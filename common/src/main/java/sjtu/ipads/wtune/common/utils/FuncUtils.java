package sjtu.ipads.wtune.common.utils;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public interface FuncUtils {
  static <T, R> List<R> listMap(Function<? super T, R> func, Iterable<T> os) {
    return StreamSupport.stream(os.spliterator(), false).map(func).collect(Collectors.toList());
  }

  static <T> T find(IPredicate<T> pred, Iterable<T> os) {
    return StreamSupport.stream(os.spliterator(), false).filter(pred).findFirst().orElse(null);
  }

  static <T> T coalesce(T val, T other) {
    return val == null ? other : val;
  }

  static <T> T coalesce(T val, Supplier<T> other) {
    return val == null ? other.get() : val;
  }
}
