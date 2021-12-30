package sjtu.ipads.wtune.common.utils;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.lang.Integer.min;

public interface ListSupport {
  static <Y> List<Y> map(int[] xs, IntFunction<Y> func) {
    final List<Y> ys = new ArrayList<>(xs.length);
    for (int x : xs) ys.add(func.apply(x));
    return ys;
  }

  static <X, Y> List<Y> map(Collection<? extends X> xs, Function<? super X, ? extends Y> func) {
    final List<Y> ys = new ArrayList<>(xs.size());
    for (X x : xs) ys.add(func.apply(x));
    return ys;
  }

  static <X, Y> List<Y> map(Iterable<? extends X> xs, Function<? super X, ? extends Y> func) {
    if (xs instanceof Collection) return map((Collection<X>) xs, func);
    return StreamSupport.stream(xs.spliterator(), false).map(func).collect(Collectors.toList());
  }

  static <T> List<T> join(List<T> xs, List<T> ys) {
    if (xs.isEmpty()) return ys;
    else if (ys.isEmpty()) return xs;
    else return new BinaryJoinedList<>(xs, ys);
  }

  @SafeVarargs
  static <T> List<T> join(List<? extends T> xs, List<? extends T> ys, List<? extends T>... ts) {
    final List<List<? extends T>> lists = new ArrayList<>(ts.length + 2);
    if (!xs.isEmpty()) lists.add(xs);
    if (!ys.isEmpty()) lists.add(ys);
    for (List<? extends T> t : ts) if (!t.isEmpty()) lists.add(t);
    return new JoinedList<>(lists);
  }

  static <T> List<T> concat(List<? extends T> ts0, List<? extends T> ts1) {
    final List<T> ts = new ArrayList<>(ts0.size() + ts1.size());
    ts.addAll(ts0);
    ts.addAll(ts1);
    return ts;
  }

  static <T> T pop(List<? extends T> xs) {
    if (xs.isEmpty()) return null;
    return xs.remove(xs.size() - 1);
  }

  static <T> List<T> filter(Iterable<? extends T> iterable, Predicate<? super T> predicate) {
    return FuncUtils.stream(iterable).filter(predicate).collect(Collectors.toList());
  }

  static <T, R> List<R> flatMap(
      Iterable<? extends T> os, Function<? super T, ? extends Iterable<R>> func) {
    return FuncUtils.stream(os)
        .map(func)
        .flatMap(FuncUtils::stream)
        .collect(Collectors.toCollection(ArrayList::new));
  }

  static <T, R> List<R> linkedListFlatMap(
      Iterable<? extends T> os, Function<? super T, ? extends Iterable<R>> func) {
    return FuncUtils.stream(os)
        .map(func)
        .flatMap(FuncUtils::stream)
        .collect(Collectors.toCollection(LinkedList::new));
  }

  static <P0, P1, R> List<R> zipMap(
      Iterable<? extends P0> l0,
      Iterable<? extends P1> l1,
      BiFunction<? super P0, ? super P1, ? extends R> func) {
    final List<R> ret;
    if (l0 instanceof Collection && l1 instanceof Collection) {
      final int size0 = ((Collection) l0).size();
      final int size1 = ((Collection) l1).size();
      ret = new ArrayList<>(min(size0, size1));
    } else {
      ret = new ArrayList<>();
    }

    final Iterator<? extends P0> iter0 = l0.iterator();
    final Iterator<? extends P1> iter1 = l1.iterator();
    while (iter0.hasNext() && iter1.hasNext()) {
      final P0 x = iter0.next();
      final P1 y = iter1.next();
      ret.add(func.apply(x, y));
    }
    return ret;
  }
}
