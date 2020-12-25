package sjtu.ipads.wtune.symsolver.utils.impl;

import sjtu.ipads.wtune.symsolver.utils.DisjointSet;

import java.util.Arrays;
import java.util.Comparator;
import java.util.NoSuchElementException;

public class BoundedDisjointSet<T> implements DisjointSet<T> {
  private static final Comparator<Object> HASH_CMP =
      Comparator.comparingInt(System::identityHashCode);

  private final T[] objs;
  private final int[] data;

  private BoundedDisjointSet(T[] objs) {
    Arrays.sort(objs, HASH_CMP);

    this.objs = objs;
    this.data = new int[objs.length];

    reset();
  }

  public static <T> BoundedDisjointSet<T> build(T[] objs) {
    return new BoundedDisjointSet<>(objs);
  }

  private int find(int i) {
    return data[i] == i ? i : (data[i] = find(data[i]));
  }

  private boolean isConnected0(int i, int j) {
    return find(i) == find(j);
  }

  private void connect0(int i, int j) {
    data[find(i)] = find(j);
  }

  private int indexOf(Object obj) {
    final int idx = Arrays.binarySearch(objs, obj, HASH_CMP);

    if (idx < 0 || objs[idx] != obj)
      throw new NoSuchElementException(obj + " not in the predefined set of members");

    return idx;
  }

  @Override
  public void connect(T x, T y) {
    connect0(indexOf(x), indexOf(y));
  }

  @Override
  public boolean isConnected(T x, T y) {
    return isConnected0(indexOf(x), indexOf(y));
  }

  @Override
  public int[] grouping() {
    final int[] assigns = new int[objs.length];
    for (int i = 0, bound = objs.length; i < bound; i++) {
      if (assigns[i] != 0) continue;
      final int assign = assigns[i] = i + 1;
      for (int j = i + 1; j < bound; j++) if (isConnected0(i, j)) assigns[j] = assign;
    }
    return assigns;
  }

  @Override
  public void reset() {
    for (int i = 0, bound = data.length; i < bound; i++) data[i] = i;
  }
}
