package sjtu.ipads.wtune.common.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public interface ListSupport {
  static <T> List<T> fromIterable(Iterable<T> iterable) {
    final List<T> list = new ArrayList<>();
    for (T t : iterable) list.add(t);
    return list;
  }

  static <X, Y> List<Y> map(Collection<X> xs, Function<X, Y> func) {
    final List<Y> ys = new ArrayList<>(xs.size());
    for (X x : xs) ys.add(func.apply(x));
    return ys;
  }

  static <Y> List<Y> map(int[] xs, IntFunction<Y> func) {
    final List<Y> ys = new ArrayList<>(xs.length);
    for (int x : xs) ys.add(func.apply(x));
    return ys;
  }

  static <X, Y> List<Y> map(Iterable<X> xs, Function<X, Y> func) {
    if (xs instanceof Collection) return map((Collection<X>) xs, func);
    return StreamSupport.stream(xs.spliterator(), false).map(func).collect(Collectors.toList());
  }

  static <T> List<T> join(List<T> xs, List<T> ys) {
    if (xs.isEmpty()) return ys;
    else if (ys.isEmpty()) return xs;
    else return new BinaryJoinedList<>(xs, ys);
  }

  @SafeVarargs
  static <T> List<T> join(List<T> xs, List<T> ys, List<T>... ts) {
    final List<List<T>> lists = new ArrayList<>(ts.length + 2);
    if (!xs.isEmpty()) lists.add(xs);
    if (!ys.isEmpty()) lists.add(ys);
    for (List<T> t : ts) if (!t.isEmpty()) lists.add(t);
    return new JoinedList<>(lists);
  }
}
