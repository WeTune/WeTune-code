package sjtu.ipads.wtune.common.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface FuncUtils {
  static <T> Predicate<T> tautology() {
    return ignored -> true;
  }

  static <T, R> Function<T, R> dumb(Supplier<R> supplier) {
    return t -> supplier.get();
  }

  static <T, R> List<R> listMap(Function<? super T, R> func, Iterable<T> os) {
    return StreamSupport.stream(os.spliterator(), false).map(func).collect(Collectors.toList());
  }

  static <T, R> Set<R> setMap(Function<? super T, R> func, Iterable<T> os) {
    return StreamSupport.stream(os.spliterator(), false).map(func).collect(Collectors.toSet());
  }

  static <T, R, C extends Collection<R>> C collectionMap(
      Function<? super T, R> func, Iterable<T> os, Supplier<C> supplier) {
    return StreamSupport.stream(os.spliterator(), false)
        .map(func)
        .collect(Collectors.toCollection(supplier));
  }

  static <T> T find(IPredicate<T> pred, Iterable<T> os) {
    return StreamSupport.stream(os.spliterator(), false).filter(pred).findFirst().orElse(null);
  }

  static <P1, P2, R> Function<P1, Function<P2, R>> curry(BiFunction<P1, P2, R> func) {
    return p1 -> p2 -> func.apply(p1, p2);
  }

  static <P1, P2, R> Function<P2, R> parital(BiFunction<P1, P2, R> func, P1 p1) {
    return p2 -> func.apply(p1, p2);
  }

  static <P, R> Function<P, R> func(Function<P, R> func) {
    return func;
  }

  static <T> List<T> listConcat(List<T> ts0, List<T> ts1) {
    final List<T> ts = new ArrayList<>(ts0.size() + ts1.size());
    ts.addAll(ts0);
    ts.addAll(ts1);
    return ts;
  }

  static <T> Stream<T> stream(Iterable<T> iterable) {
    return StreamSupport.stream(iterable.spliterator(), false);
  }

  static <T> T[] asArray(T... vals) {
    return vals;
  }

  static <T> T coalesce(T... vals) {
    for (T val : vals) if (val != null) return val;
    return null;
  }

  static <T> T coalesce(T val, T other) {
    return val == null ? other : val;
  }

  static <T> T coalesce(T val, Supplier<T> other) {
    return val == null ? other.get() : val;
  }
}
