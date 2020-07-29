package sjtu.ipads.wtune.common.utils;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

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

  static <T> Optional<T> safeGet(T[] arr, int idx) {
    if (idx >= arr.length) return Optional.empty();
    return Optional.of(arr[idx]);
  }

  static <T> Optional<T> safeGet(List<T> list, int idx) {
    if (idx >= list.size()) return Optional.empty();
    return Optional.of(list.get(idx));
  }

  static boolean isEmpty(Collection<?> list) {
    return list == null || list.isEmpty();
  }
}
