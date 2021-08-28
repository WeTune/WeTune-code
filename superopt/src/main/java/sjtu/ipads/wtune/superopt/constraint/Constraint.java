package sjtu.ipads.wtune.superopt.constraint;

import sjtu.ipads.wtune.superopt.fragment.Symbol;
import sjtu.ipads.wtune.superopt.fragment.SymbolNaming;

public interface Constraint {
  enum Kind {
    TableEq(2),
    AttrsEq(2),
    PredicateEq(2),
    AttrsSub(2),
    Unique(2),
    NotNull(2),
    Reference(4),
    AttrsFrom(2),
    ; // DON'T change the order. Some implementations trick depends on this.

    private final int numSyms;

    Kind(int numSyms) {
      this.numSyms = numSyms;
    }

    public int numSyms() {
      return numSyms;
    }

    public boolean isEq() {
      return this == TableEq || this == AttrsEq || this == PredicateEq;
    }
  }

  Kind kind();

  Symbol[] symbols();

  String canonicalStringify(SymbolNaming naming);

  StringBuilder stringify(SymbolNaming naming, StringBuilder builder);

  default String stringify(SymbolNaming naming) {
    return stringify(naming, new StringBuilder()).toString();
  }

  static Constraint parse(String str, SymbolNaming naming) {
    return ConstraintImpl.parse(str, naming);
  }

  static Constraint mk(Kind kind, Symbol... symbols) {
    return new ConstraintImpl(kind, symbols);
  }
}
