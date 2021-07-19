package sjtu.ipads.wtune.common.utils;

public interface NameSequence {
  String next();

  static NameSequence mkIndexed(String prefix, int baseIndex) {
    return new IndexedNameSequence(prefix, baseIndex);
  }
}
