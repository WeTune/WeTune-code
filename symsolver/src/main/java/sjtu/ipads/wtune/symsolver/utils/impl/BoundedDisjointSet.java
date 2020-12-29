package sjtu.ipads.wtune.symsolver.utils.impl;

import sjtu.ipads.wtune.symsolver.utils.DisjointSet;
import sjtu.ipads.wtune.symsolver.utils.Indexed;

import java.util.Arrays;
import java.util.NoSuchElementException;

import static java.util.Arrays.binarySearch;
import static sjtu.ipads.wtune.symsolver.utils.Indexed.INDEX_CMP;
import static sjtu.ipads.wtune.symsolver.utils.Indexed.isCanonicalIndexed;

public class BoundedDisjointSet<T extends Indexed> implements DisjointSet<T> {

  private final T[] objs;
  private final int[] data;
  private final boolean useFastIndex;

  private BoundedDisjointSet(T[] objs) {
    objs = Arrays.copyOf(objs, objs.length);
    Arrays.sort(objs, INDEX_CMP);

    this.objs = objs;
    this.data = new int[objs.length];
    this.useFastIndex = isCanonicalIndexed(objs);

    reset();
  }

  public static <T extends Indexed> BoundedDisjointSet<T> build(T[] objs) {
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

  private int indexOf(T obj) {
    final int idx = useFastIndex ? obj.index() : binarySearch(objs, obj, INDEX_CMP);

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
    Arrays.fill(assigns, -1);
    for (int i = 0, bound = objs.length; i < bound; i++) {
      if (assigns[i] != -1) continue;
      assigns[i] = i;
      for (int j = i + 1; j < bound; j++) if (isConnected0(i, j)) assigns[j] = i;
    }
    return assigns;
  }

  @Override
  public void reset() {
    for (int i = 0, bound = data.length; i < bound; i++) data[i] = i;
  }
}
