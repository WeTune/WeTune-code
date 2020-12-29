package sjtu.ipads.wtune.symsolver.utils;

import java.util.Comparator;

public interface Indexed extends Comparable<Indexed> {
  Comparator<Indexed> INDEX_CMP = Comparator.comparingInt(Indexed::index);

  static boolean isCanonicalIndexed(Indexed[] xs) {
    for (int i = 0, bound = xs.length; i < bound; i++) if (i != xs[i].index()) return false;
    return true;
  }

  int index();

  void setIndex(int index);

  @Override
  default int compareTo(Indexed o) {
    return Integer.compare(index(), o.index());
  }
}
