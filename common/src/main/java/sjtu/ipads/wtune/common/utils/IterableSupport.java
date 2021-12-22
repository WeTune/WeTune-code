package sjtu.ipads.wtune.common.utils;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public interface IterableSupport {
  static <T> T linearFind(Iterable<? extends T> os, Predicate<? super T> pred) {
    for (T o : os) if (pred.test(o)) return o;
    return null;
  }

  static <T> Iterable<T> lazyFilter(Iterable<? extends T> os, Predicate<? super T> predicate) {
    return () -> new FilteredIterator<>(os.iterator(), predicate);
  }

  static <T> boolean any(Iterable<? extends T> xs, Predicate<? super T> check) {
    return xs != null && FuncUtils.stream(xs).anyMatch(check);
  }

  static <T> boolean all(Iterable<? extends T> xs, Predicate<? super T> check) {
    return xs == null || FuncUtils.stream(xs).allMatch(check);
  }

  static <T> boolean none(Iterable<? extends T> xs, Predicate<? super T> check) {
    return xs == null || FuncUtils.stream(xs).noneMatch(check);
  }

  static <X, Y> Iterable<Pair<X, Y>> zip(Iterable<? extends X> xs, Iterable<? extends Y> ys) {
    return () -> new ZippedIterator<>(xs.iterator(), ys.iterator());
  }

  static <P0, P1> void zip(
      Iterable<? extends P0> l0,
      Iterable<? extends P1> l1,
      BiConsumer<? super P0, ? super P1> func) {
    final Iterator<? extends P0> it0 = l0.iterator();
    final Iterator<? extends P1> it1 = l1.iterator();
    while (it0.hasNext() && it1.hasNext()) func.accept(it0.next(), it1.next());
  }
}
