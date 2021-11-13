package sjtu.ipads.wtune.common.utils;

public interface ArraySupport {
  static int sequentialFind(int[] arr, int target, int fromIndex) {
    for (int i = fromIndex, bound = arr.length; i < bound; i++) if (arr[i] == target) return i;
    return -1;
  }
}
