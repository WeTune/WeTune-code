package sjtu.ipads.wtune.superopt.fragment1;

/** Identity-based immutable class. */
public interface Symbol {
  enum Kind {
    TABLE,
    ATTRS,
    PRED
  }

  Kind kind();

  static Symbol mk(Kind kind) {
    return new SymbolImpl(kind);
  }
}
