package sjtu.ipads.wtune.common.utils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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

  static <T, R, C extends Collection<R>> C collectionFlatMap(
      Function<? super T, ? extends Iterable<R>> func, T[] arr, Supplier<C> supplier) {
    return stream(Arrays.asList(arr))
        .map(func)
        .flatMap(FuncUtils::stream)
        .collect(Collectors.toCollection(supplier));
  }

  static <T, R> List<R> listMap(Function<? super T, ? extends R> func, Iterable<T> os) {
    return collectionMap(func, os, ArrayList::new);
  }

  @SafeVarargs
  static <T, R> List<R> listMap(Function<? super T, R> func, T... os) {
    return listMap(func, Arrays.asList(os));
  }

  static <T, R> List<R> listFlatMap(
      Function<? super T, ? extends Iterable<R>> func, Iterable<T> os) {
    return collectionFlatMap(func, os, ArrayList::new);
  }

  static <T, R> List<R> listFlatMap(Function<? super T, ? extends Iterable<R>> func, T... os) {
    return listFlatMap(func, Arrays.asList(os));
  }

  static <T> boolean all(Iterable<T> xs, Predicate<T> check) {
    return xs == null || stream(xs).allMatch(check);
  }

  static <T> boolean any(Iterable<T> xs, Predicate<T> check) {
    return xs != null && stream(xs).anyMatch(check);
  }

  static <T> boolean none(Iterable<T> xs, Predicate<T> check) {
    return xs == null || stream(xs).noneMatch(check);
  }

  static <P0, P1, R> List<R> zipMap(
      Collection<P0> l0, Collection<P1> l1, BiFunction<? super P0, ? super P1, R> func) {
    final int bound = Math.min(l0.size(), l1.size());
    final List<R> list = new ArrayList<>(bound);
    final Iterator<P0> it0 = l0.iterator();
    final Iterator<P1> it1 = l1.iterator();
    for (int i = 0; i < bound; i++) list.add(func.apply(it0.next(), it1.next()));
    return list;
  }

  static <P0, P1> void zipForEach(
      BiConsumer<? super P0, ? super P1> func, Collection<P0> l0, Collection<P1> l1) {
    final int bound = Math.min(l0.size(), l1.size());
    final Iterator<P0> it0 = l0.iterator();
    final Iterator<P1> it1 = l1.iterator();
    for (int i = 0; i < bound; i++) func.accept(it0.next(), it1.next());
  }

  static <T> List<T> listFilter(Iterable<T> os, Predicate<? super T> func) {
    return stream(os).filter(func).collect(Collectors.toList());
  }

  static <T, C extends Collection<T>> C collectionFilter(
      Predicate<? super T> func, Iterable<T> os, Supplier<C> supplier) {
    return stream(os).filter(func).collect(Collectors.toCollection(supplier));
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

  static <T> int indexOf(Predicate<T> pred, Iterable<T> os) {
    int index = 0;
    for (T o : os) {
      if (pred.test(o)) return index;
      index++;
    }
    return -1;
  }

  static <T> T find(Predicate<T> pred, Iterable<T> os) {
    for (T o : os) if (pred.test(o)) return o;
    return null;
  }

  @SafeVarargs
  static <T> T find(Predicate<T> pred, T... ts) {
    for (T t : ts) if (pred.test(t)) return t;
    return null;
  }

  static <T> Stream<T> stream(Iterable<T> iterable) {
    if (iterable instanceof Collection) return ((Collection<T>) iterable).stream();
    else return StreamSupport.stream(iterable.spliterator(), false);
  }

  static <T> Stream<T> stream(T[] array) {
    return Arrays.stream(array);
  }
}
