package sjtu.ipads.wtune.common.utils;

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

  static <T> T threadLocal(ThreadLocal<T> threadLocal, Supplier<T> supplier) {
    T t = threadLocal.get();
    if (t == null) threadLocal.set(t = supplier.get());
    return t;
  }
}
