package sjtu.ipads.wtune.superopt.fragment;

/** Identity-based immutable class. */
public interface Symbol {
  enum Kind {
    TABLE,
    ATTRS,
    PRED,
    SCHEMA
  }

  Kind kind();

  Symbols ctx();

  static Symbol mk(Kind kind, Symbols ctx) {
    return new SymbolImpl(kind, ctx);
  }
}
