package sjtu.ipads.wtune.common.utils;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

public interface ArraySupport {
  static int sequentialFind(int[] arr, int target, int fromIndex) {
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

  @SuppressWarnings("unchecked")
  static <T> T[] mkArray(Class<T> cls, int n) {
    return (T[]) Array.newInstance(cls, n);
  }
}
