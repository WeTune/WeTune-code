package sjtu.ipads.wtune.superopt.fragment1;

/** Identity-based immutable class. */
public interface Symbol {
  enum Kind {
    TABLE,
    ATTRS,
    PRED
  }

  Kind kind();

  Symbols ctx();

  static Symbol mk(Kind kind, Symbols ctx) {
    return new SymbolImpl(kind, ctx);
  }
}
