package sjtu.ipads.wtune.common.utils;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Supplier;

import static sjtu.ipads.wtune.common.utils.FuncUtils.stream;

public interface Commons {
  /** if str[0] == '"' and str[-1] == '"', return str[1:-2] */
  static String unquoted(String str) {
    return unquoted(str, '"');
  }

  /** if str[0] == quota and str[-1] == quota, return str[1:-2] */
  static String unquoted(String str, char quota) {
    if (str == null) return null;
    final int length = str.length();
    if (length <= 1) return str;

    final char c0 = str.charAt(0);
    final char ce = str.charAt(length - 1);
    return quota == c0 && quota == ce ? str.substring(1, length - 1) : str;
  }

  /** Helper to make compiler happy. */
  static <T> T assertFalse() {
    assert false;
    return null;
  }

  static StringBuilder trimTrailing(StringBuilder sb, int i) {
    return sb.delete(sb.length() - i, sb.length());
  }

  static String surround(String str, char quota) {
    return quota + str + quota;
  }

  @SafeVarargs
  static <T> T coalesce(T... vals) {
    for (T val : vals) if (val != null) return val;
    return null;
  }

  static <T> T coalesce(T val, Supplier<T> other) {
    return val == null ? other.get() : val;
  }

  @SuppressWarnings("unchecked")
  static <T> T[] makeArray(Class<T> cls, int n) {
    return (T[]) Array.newInstance(cls, n);
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

  static <T> T[] sorted(T[] arr, Comparator<? super T> comparator) {
    Arrays.sort(arr, comparator);
    return arr;
  }

  @SuppressWarnings("unchecked")
  static <T> T[] subArray(T[] arr, int start) {
    if (start < 0 || start > arr.length) throw new IndexOutOfBoundsException();
    final T[] subArr =
        (T[]) Array.newInstance(arr.getClass().getComponentType(), arr.length - start);
    System.arraycopy(arr, start, subArr, 0, subArr.length);
    return subArr;
  }

  @SuppressWarnings("unchecked")
  static <T> T[] maskArray(T[] arr, int seed) {
    final T[] ret =
        (T[]) Array.newInstance(arr.getClass().getComponentType(), Integer.bitCount(seed));

    final int wall = arr.length - 1;
    for (int i = 0, j = 0, bound = arr.length; i < bound; i++)
      if ((seed & (1 << (wall - i))) != 0) ret[j++] = arr[i];

    return ret;
  }

  /**
   * Select elements in `arr` according to 1 bit in `seed`, and comprise a new array. <br>
   * Note: that the lowest bit corresponds to the last element in array. The bits that higher than
   * `arr.length` is ignored.<br>
   * Example: given arr=[1,2,3] seed=0b011, return value is [2,3]
   */
  @SuppressWarnings("unchecked")
  static <T> T[] maskArray(T[] arr, long seed) {
    final T[] ret = (T[]) Array.newInstance(arr.getClass().getComponentType(), Long.bitCount(seed));

    final int wall = arr.length - 1;
    for (int i = 0, j = 0, bound = arr.length; i < bound; i++)
      if ((seed & (1L << (wall - i))) != 0) ret[j++] = arr[i];

    return ret;
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

  @SuppressWarnings("unchecked")
  static <T> T[] arrayConcat(T[] arr, T... vs) {
    final T[] ret =
        (T[]) Array.newInstance(arr.getClass().getComponentType(), arr.length + vs.length);
    System.arraycopy(arr, 0, ret, 0, arr.length);
    System.arraycopy(vs, 0, ret, arr.length, vs.length);
    return ret;
  }

  @SuppressWarnings("unchecked")
  static <T> T[] arrayConcat(T[] arr1, T[] arr2, T... vs) {
    final T[] ret =
        (T[])
            Array.newInstance(
                arr1.getClass().getComponentType(), arr1.length + arr2.length + vs.length);
    System.arraycopy(arr1, 0, ret, 0, arr1.length);
    System.arraycopy(arr2, 0, ret, arr1.length, arr2.length);
    System.arraycopy(vs, 0, ret, arr1.length + arr2.length, vs.length);
    return ret;
  }

  @SuppressWarnings("unchecked")
  static <T> T[] arrayConcat(T[]... arrs) {
    final int length = stream(arrs).mapToInt(it -> it.length).sum();
    final T[] ret =
        (T[]) Array.newInstance(arrs.getClass().getComponentType().getComponentType(), length);

    int base = 0;
    for (final T[] arr : arrs) {
      System.arraycopy(arr, 0, ret, base, arr.length);
      base += arr.length;
    }
    return ret;
  }

  static <T> List<T> listConcat(List<T> ts0, List<T> ts1) {
    final List<T> ts = new ArrayList<>(ts0.size() + ts1.size());
    ts.addAll(ts0);
    ts.addAll(ts1);
    return ts;
  }

  static <T> List<T> listConcatView(List<T>... ts) {
    return new ConcatenatedList<>(Arrays.asList(ts));
  }

  static <T> List<T> listSort(List<T> arr, Comparator<? super T> comparator) {
    arr.sort(comparator);
    return arr;
  }

  static <T> T echo(T t) {
    System.out.println(t);
    return t;
  }
}
