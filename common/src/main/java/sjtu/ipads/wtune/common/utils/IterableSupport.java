package sjtu.ipads.wtune.common.utils;

import org.apache.commons.lang3.tuple.Pair;

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

  static <T> boolean none(Iterable<T> xs, Predicate<T> check) {
    return xs == null || FuncUtils.stream(xs).noneMatch(check);
  }

  static <T> boolean all(Iterable<T> xs, Predicate<T> check) {
    return xs == null || FuncUtils.stream(xs).allMatch(check);
  }

  static <X, Y> Iterable<Pair<X, Y>> zip(Iterable<X> xs, Iterable<Y> ys) {
    return () -> new ZippedIterator<>(xs.iterator(), ys.iterator());
  }
}
