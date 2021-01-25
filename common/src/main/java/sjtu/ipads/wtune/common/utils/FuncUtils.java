package sjtu.ipads.wtune.common.utils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface FuncUtils {
  static <T, R> IFunction<T, R> dumb(Supplier<R> supplier) {
    return t -> supplier.get();
  }

  static <P, R> IFunction<P, R> func(IFunction<P, R> func) {
    return func;
  }

  static <P0, P1, R> IBiFunction<P0, P1, R> func2(IBiFunction<P0, P1, R> func2) {
    return func2;
  }

  static <R> ISupplier<R> supplier(ISupplier<R> supplier) {
    return supplier;
  }

  static <P> Predicate<P> pred(Predicate<P> pred) {
    return pred;
  }

  static <T, R, C extends Collection<R>> C collectionMap(
      Function<? super T, R> func, Iterable<T> os, Supplier<C> supplier) {
    return stream(os).map(func).collect(Collectors.toCollection(supplier));
  }

  static <T, R> List<R> listMap(Function<? super T, R> func, Iterable<T> os) {
    return collectionMap(func, os, ArrayList::new);
  }

  @SafeVarargs
  static <T, R> List<R> listMap(Function<? super T, R> func, T... os) {
    return stream(os).map(func).collect(Collectors.toList());
  }

  static <T> List<T> listFilter(Predicate<? super T> func, Iterable<T> os) {
    return stream(os).filter(func).collect(Collectors.toList());
  }

  @SafeVarargs
  static <T, R> R[] arrayMap(Function<? super T, R> func, Class<R> retType, T... ts) {
    final R[] rs = Commons.makeArray(retType, ts.length);
    for (int i = 0, bound = ts.length; i < bound; i++) rs[i] = func.apply(ts[i]);
    return rs;
  }

  static <T, R> R[] arrayMap(Function<? super T, R> func, Class<R> retType, Collection<T> ts) {
    final R[] rs = Commons.makeArray(retType, ts.size());
    int i = 0;
    for (T t : ts) rs[i++] = func.apply(t);
    return rs;
  }

  @SafeVarargs
  @SuppressWarnings("unchecked")
  static <T> T[] arrayFilter(Predicate<T> test, T... arr) {
    return stream(arr)
        .filter(test)
        .toArray(n -> (T[]) Array.newInstance(arr.getClass().getComponentType(), n));
  }

  static <T> T[] generate(int n, Class<T> retType, IntFunction<T> func) {
    return IntStream.range(0, n).mapToObj(func).toArray(len -> Commons.makeArray(retType, len));
  }

  static <T> T find(Predicate<T> pred, Iterable<T> os) {
    return stream(os).filter(pred).findFirst().orElse(null);
  }

  @SafeVarargs
  static <T> T find(Predicate<T> pred, T... ts) {
    for (T t : ts) if (pred.test(t)) return t;
    return null;
  }

  static <T> Stream<T> stream(Iterable<T> iterable) {
    return StreamSupport.stream(iterable.spliterator(), false);
  }

  static <T> Stream<T> stream(T[] array) {
    return Arrays.stream(array);
  }
}
