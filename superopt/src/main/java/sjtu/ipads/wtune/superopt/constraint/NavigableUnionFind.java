package sjtu.ipads.wtune.superopt.constraint;

import java.util.Arrays;

/**
 * A customized union-find that support navigation, i.e., provides a method to retrieve the next
 * equivalence class.
 *
 * <p>For this purpose, this data structure has a restriction over an ordinary union-find: the
 * indices range of equivalence classes, except singleton ones, are disallowed to overlap. For
 * example, the following eq-partition is illegal: ( (0,2), (1,3) ).
 *
 * <p>Thus, the equivalence relation is designed to stored as backward linked-list. For example, we
 * have equivalence class ((0),(1,2),(3,5,6),(4)), the underlying array is [-1,2,-1,5,-1,6,-1]
 *
 * <p>The pivot of each eq-class is the min element, which represents the eq-class. So the first
 * eq-class is always 0.
 *
 * <p>The root of each eq-class is the max element, of which the next index must be the pivot of
 * next eq-class.
 *
 * <p>A merge leading to overlapped range is not permitted.
 */
class NavigableUnionFind {
  private final int[] data;

  NavigableUnionFind(int size) {
    this.data = new int[size];
    for (int i = 0, bound = data.length; i < bound; i++) data[i] = i;
    Arrays.fill(data, -1);
  }

  int firstEqClass() {
    return 0;
  }

  int nextEqClass(int pivot) {
    final int root = rootOf(pivot) + 1;
    if (root >= data.length) return -1;
    else return root;
  }

  void merge(int pivot0, int pivot1) {
    data[rootOf(pivot0)] = pivot1;
  }

  private int rootOf(int pivot) {
    int cursor = data[pivot], root = pivot;
    while (cursor != -1) {
      root = cursor;
      cursor = data[cursor];
    }
    return root;
  }
}
