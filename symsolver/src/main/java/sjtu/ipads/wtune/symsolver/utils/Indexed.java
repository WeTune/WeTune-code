package sjtu.ipads.wtune.symsolver.utils;

public interface Indexed extends Comparable<Indexed> {
  int UNKNOWN_INDEX = -1;

  static boolean isCanonicalIndexed(Indexed[] xs) {
    for (int i = 0, bound = xs.length; i < bound; i++) if (i != xs[i].index()) return false;
    return true;
  }

  int index();

  void setIndex(int index);

  @Override
  default int compareTo(Indexed o) {
    if (index() == UNKNOWN_INDEX || o.index() == UNKNOWN_INDEX)
      throw new IllegalArgumentException("index is not yet set");

    return Integer.compare(index(), o.index());
  }
}
