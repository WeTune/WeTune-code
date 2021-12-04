package sjtu.ipads.wtune.common.utils;

import java.util.function.Predicate;

public interface IterableSupport {
  static <T> T linearFind(Iterable<T> os, Predicate<T> pred) {
    for (T o : os) if (pred.test(o)) return o;
    return null;
  }

  static <T> boolean any(Iterable<T> xs, Predicate<T> check) {
    return xs != null && FuncUtils.stream(xs).anyMatch(check);
  }

  static <T> Iterable<T> lazyFilter(Iterable<T> os, Predicate<? super T> predicate) {
    return () -> new FilteredIterator<>(os.iterator(), predicate);
  }
}
