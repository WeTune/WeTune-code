package sjtu.ipads.wtune.common.utils;

import java.lang.reflect.Array;
import java.util.*;
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
    return stream(os).map(func).collect(Collectors.toList());
  }

  @SafeVarargs
  static <T, R> List<R> listMap(Function<? super T, R> func, T... os) {
    return stream(os).map(func).collect(Collectors.toList());
  }

  static <T, R, C extends Collection<R>> C collectionMap(
      Function<? super T, R> func, Iterable<T> os, Supplier<C> supplier) {
    return StreamSupport.stream(os.spliterator(), false)
        .map(func)
        .collect(Collectors.toCollection(supplier));
  }

  @SafeVarargs
  @SuppressWarnings("unchecked")
  static <T, R> R[] arrayMap(Function<? super T, R> func, Class<R> retType, T... ts) {
    final R[] rs = (R[]) Array.newInstance(retType, ts.length);
    for (int i = 0, bound = ts.length; i < bound; i++) rs[i] = func.apply(ts[i]);
    return rs;
  }

  @SafeVarargs
  @SuppressWarnings("unchecked")
  static <T> T[] arrayFilter(Predicate<T> test, T... arr) {
    return stream(arr)
        .filter(test)
        .toArray(n -> (T[]) Array.newInstance(arr.getClass().getComponentType(), n));
  }

  static <T> List<T> listConcat(List<T> ts0, List<T> ts1) {
    final List<T> ts = new ArrayList<>(ts0.size() + ts1.size());
    ts.addAll(ts0);
    ts.addAll(ts1);
    return ts;
  }

  static <T> T find(IPredicate<T> pred, Iterable<T> os) {
    return StreamSupport.stream(os.spliterator(), false).filter(pred).findFirst().orElse(null);
  }

  @SafeVarargs
  static <T> T find(Predicate<T> pred, T... ts) {
    for (T t : ts) if (pred.test(t)) return t;
    return null;
  }

  static <P1, P2, R> Function<P2, R> partial(BiFunction<P1, P2, R> func, P1 p1) {
    return p2 -> func.apply(p1, p2);
  }

  static <P, R> Function<P, R> func(Function<P, R> func) {
    return func;
  }

  static <T> Stream<T> stream(Iterable<T> iterable) {
    return StreamSupport.stream(iterable.spliterator(), false);
  }

  static <T> Stream<T> stream(T[] array) {
    return Arrays.stream(array);
  }

  @SafeVarargs
  static <T> T[] asArray(T... vals) {
    return vals;
  }

  @SuppressWarnings("unchecked")
  static <T> T[] repeat(T value, int times) {
    final T[] arr = (T[]) Array.newInstance(value.getClass(), times);
    Arrays.fill(arr, value);
    return arr;
  }

  @SuppressWarnings("unchecked")
  static <T> T[] arrayConcat(T[] arr1, T[] arr2) {
    final T[] arr =
        (T[]) Array.newInstance(arr1.getClass().getComponentType(), arr1.length + arr2.length);
    System.arraycopy(arr1, 0, arr, 0, arr1.length);
    System.arraycopy(arr2, 0, arr, arr1.length, arr2.length);
    return arr;
  }

  static <T> T[] sorted(T[] arr, Comparator<? super T> comparator) {
    Arrays.sort(arr, comparator);
    return arr;
  }

  @SafeVarargs
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

  static <T> T echo(T t) {
    System.out.println(t);
    return t;
  }

  static boolean isSubSequence(Object[] container, Object[] contained) {
    if (contained.length > container.length) return false;
    if (contained.length == 0) return true;

    for (int i = 0, j = 0; i < container.length; i++)
      if (Objects.equals(container[i], contained[j])) {
        j++;
        if (j >= contained.length) return true;
      }

    return false;
  }
}
