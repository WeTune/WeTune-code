package sjtu.ipads.wtune.symsolver.utils;

import sjtu.ipads.wtune.symsolver.utils.impl.BoundedDisjointSet;

public interface DisjointSet<T> {
  void connect(T x, T y);

  boolean isConnected(T x, T y);

  void reset();

  int[] grouping();

  static <T> DisjointSet<T> fromBoundedMembers(T[] objs) {
    return BoundedDisjointSet.build(objs);
  }
}
