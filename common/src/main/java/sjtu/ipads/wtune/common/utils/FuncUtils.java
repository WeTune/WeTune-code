package sjtu.ipads.wtune.common.utils;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface FuncUtils {
  static <T> Predicate<T> tautology() {
    return ignored -> true;
  }

  static <T, R> IFunction<T, R> deaf(Supplier<R> supplier) {
    return t -> supplier.get();
  }

  static <T> IConsumer<T> dumb(Function<T, ?> func) {
    return func::apply;
  }

  static <P> IConsumer<P> consumer(IConsumer<P> consumer) {
    return consumer;
  }

  static <P0, P1> IBiConsumer<P0, P1> consumer2(IBiConsumer<P0, P1> consumer) {
    return consumer;
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
      Function<? super T, ? extends R> func, Iterable<T> os, Supplier<C> supplier) {
    return stream(os).map(func).collect(Collectors.toCollection(supplier));
  }

  static <T, R, C extends Collection<R>> C collectionFlatMap(
      Function<? super T, ? extends Iterable<R>> func, Iterable<T> os, Supplier<C> supplier) {
    return stream(os)
        .map(func)
        .flatMap(FuncUtils::stream)
        .collect(Collectors.toCollection(supplier));
  }

  static <T, R> Set<R> setMap(Iterable<T> os, Function<? super T, ? extends R> func) {
    return collectionMap(func, os, HashSet::new);
  }

  static <T> List<T> listFilter(Iterable<T> os, Predicate<? super T> func) {
    return stream(os).filter(func).collect(Collectors.toList());
  }

  static <T, C extends Collection<T>> C collectionFilter(
      Predicate<? super T> func, Iterable<T> os, Supplier<C> supplier) {
    return stream(os).filter(func).collect(Collectors.toCollection(supplier));
  }

  @SafeVarargs
  @SuppressWarnings("unchecked")
  static <T> T[] arrayFilter(Predicate<T> test, T... arr) {
    return stream(arr)
        .filter(test)
        .toArray(n -> (T[]) Array.newInstance(arr.getClass().getComponentType(), n));
  }

  static <T> int locate(Iterable<T> os, Predicate<T> pred) {
    int index = 0;
    for (T o : os) {
      if (pred.test(o)) return index;
      index++;
    }
    return -1;
  }

  static <T> Stream<T> stream(T[] array) {
    return Arrays.stream(array);
  }

  static <T> Stream<T> stream(Iterable<T> iterable) {
    if (iterable instanceof Collection) return ((Collection<T>) iterable).stream();
    else return StreamSupport.stream(iterable.spliterator(), false);
  }
}
