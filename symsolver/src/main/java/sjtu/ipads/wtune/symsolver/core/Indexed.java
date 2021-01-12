package sjtu.ipads.wtune.symsolver.core;

public interface Indexed extends Comparable<Indexed> {
  static <T extends Indexed> T[] number(T[] indices, int start) {
    for (T index : indices) index.setIndex(start++);
    return indices;
  }

  static boolean isCanonicalIndexed(Indexed[] xs) {
    for (int i = 0, bound = xs.length; i < bound; i++) if (i != xs[i].index()) return false;
    return true;
  }

  int index();

  void setIndex(int index);

  default boolean isIndexed() {
    return index() >= 0;
  }

  @Override
  default int compareTo(Indexed o) {
    if (!isIndexed() || !o.isIndexed()) throw new IllegalArgumentException("index is not yet set");

    return Integer.compare(index(), o.index());
  }
}
