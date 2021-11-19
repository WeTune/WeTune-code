package sjtu.ipads.wtune.common.utils;

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
}
