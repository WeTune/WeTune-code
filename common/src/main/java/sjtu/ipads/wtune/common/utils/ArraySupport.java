package sjtu.ipads.wtune.common.utils;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.StreamSupport;

import static sjtu.ipads.wtune.common.utils.PartialOrder.*;

public interface ArraySupport {
  int[] EMPTY_INT_ARRAY = new int[0];

  /**
   * Compare two arrays of bools on a partial order.
   *
   * <p>The partial order is defined as
   *
   * <ul>
   *   <li>if \forall i, bs0[i] || !bs1[i], then bs0 > bs1
   *   <li>if \forall i, bs1[i] || !bs0[i], then bs0 < bs1
   *   <li>if \forall i, bs0[i] == bs1[i], then bs0 == bs1
   *   <li>otherwise, incomparable.
   * </ul>
   */
  static PartialOrder compareBools(boolean[] bs0, boolean[] bs1) {
    if (bs0.length != bs1.length)
      throw new IllegalArgumentException("cannot compare arrays of different lengths");

    PartialOrder cmp = SAME;
    for (int i = 0, bound = bs0.length; i < bound; ++i) {
      if (bs0[i] && !bs1[i])
        if (cmp == LESS_THAN) return INCOMPARABLE;
        else cmp = GREATER_THAN;
      else if (!bs0[i] && bs1[i])
        if (cmp == GREATER_THAN) return INCOMPARABLE;
        else cmp = LESS_THAN;
    }
    return SAME;
  }

  static int linearFind(int[] arr, int target, int fromIndex) {
    if (arr == null) return -1;
    for (int i = fromIndex, bound = arr.length; i < bound; i++) if (arr[i] == target) return i;
    return -1;
  }

  static int safeGet(int[] arr, int index, int defaultVal) {
    if (arr == null || index < 0 || index >= arr.length) return defaultVal;
    else return arr[index];
  }

  static <T> T safeGet(T[] arr, int index, T defaultVal) {
    if (arr == null || index < 0 || index >= arr.length) return defaultVal;
    else return arr[index];
  }

  static <T> T linearFind(T[] ts, Predicate<T> pred) {
    for (T t : ts) if (pred.test(t)) return t;
    return null;
  }

  @SuppressWarnings("unchecked")
  static <T> T[] repeat(T value, int times) {
    final T[] arr = (T[]) Array.newInstance(value.getClass(), times);
    Arrays.fill(arr, value);
    return arr;
  }

  static <T> T[] generate(int n, IntFunction<T> func, Class<T> retType) {
    final T[] arr = mkArray(retType, n);
    for (int i = 0; i < arr.length; i++) arr[i] = func.apply(i);
    return arr;
  }

  static int[] range(int fromInclusive, int toExclusive) {
    if (toExclusive <= fromInclusive) return EMPTY_INT_ARRAY;

    final int[] arr = new int[toExclusive - fromInclusive];
    for (int i = fromInclusive; i < toExclusive; ++i) arr[i - fromInclusive] = i;
    return arr;
  }

  static <T> int[] map(Iterable<T> ts, ToIntFunction<? super T> func) {
    if (ts instanceof Collection) {
      final int[] ret = new int[((Collection<T>) ts).size()];
      int i = 0;
      for (T t : ts) ret[i++] = func.applyAsInt(t);
      return ret;

    } else {
      return FuncUtils.stream(ts).mapToInt(func).toArray();
    }
  }

  static <T> TIntSet mapSet(Iterable<T> ts, ToIntFunction<? super T> func) {
    final TIntSet ret =
        ts instanceof Collection ? new TIntHashSet(((Collection<T>) ts).size()) : new TIntHashSet();

    for (T t : ts) ret.add(func.applyAsInt(t));
    return ret;
  }

  static <T, R> R[] map(T[] ts, Function<? super T, R> func, Class<R> retType) {
    final R[] rs = mkArray(retType, ts.length);
    for (int i = 0, bound = ts.length; i < bound; i++) rs[i] = func.apply(ts[i]);
    return rs;
  }

  static <T, R> R[] map(Iterable<T> ts, Function<? super T, R> func, Class<R> retType) {
    if (ts instanceof Collection) {
      final int size = ((Collection<T>) ts).size();
      final R[] rs = mkArray(retType, size);
      int i = 0;
      for (T t : ts) rs[i++] = func.apply(t);
      return rs;

    } else {
      return StreamSupport.stream(ts.spliterator(), false)
          .map(func)
          .toArray(x -> mkArray(retType, x));
    }
  }

  static int sum(int[] arr) {
    int sum = 0;
    for (int i : arr) sum += i;
    return sum;
  }

  @SuppressWarnings("unchecked")
  static <T> T[] mkArray(Class<T> cls, int n) {
    return (T[]) Array.newInstance(cls, n);
  }
}
