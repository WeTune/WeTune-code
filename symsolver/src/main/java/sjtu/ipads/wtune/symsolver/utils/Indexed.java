package sjtu.ipads.wtune.symsolver.utils;

public interface Indexed extends Comparable<Indexed> {
  int index();

  void setIndex(int index);

  @Override
  default int compareTo(Indexed o) {
    return Integer.compare(index(), o.index());
  }
}
