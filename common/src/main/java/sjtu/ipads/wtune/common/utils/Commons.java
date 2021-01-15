package sjtu.ipads.wtune.common.utils;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Supplier;

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
  static <T> T[] arrayConcat(T[] arr1, T[] arr2) {
    final T[] arr =
        (T[]) Array.newInstance(arr1.getClass().getComponentType(), arr1.length + arr2.length);
    System.arraycopy(arr1, 0, arr, 0, arr1.length);
    System.arraycopy(arr2, 0, arr, arr1.length, arr2.length);
    return arr;
  }

  static <T> List<T> listConcat(List<T> ts0, List<T> ts1) {
    final List<T> ts = new ArrayList<>(ts0.size() + ts1.size());
    ts.addAll(ts0);
    ts.addAll(ts1);
    return ts;
  }

  static <T> T echo(T t) {
    System.out.println(t);
    return t;
  }
}
