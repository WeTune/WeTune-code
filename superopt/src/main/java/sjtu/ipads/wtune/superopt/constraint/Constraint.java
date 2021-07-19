package sjtu.ipads.wtune.superopt.constraint;

import sjtu.ipads.wtune.superopt.fragment1.Symbol;
import sjtu.ipads.wtune.superopt.fragment1.SymbolNaming;

public interface Constraint {
  enum Kind {
    TableEq(2),
    AttrsEq(2),
    PredicateEq(2),
    AttrsFrom(2),
    AttrsSub(2),
    Reference(4),
    Unique(2);

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

  StringBuilder stringify(SymbolNaming naming, StringBuilder builder);

  default String stringify(SymbolNaming naming) {
    return stringify(naming, new StringBuilder()).toString();
  }

  static Constraint parse(String str, SymbolNaming naming) {
    return ConstraintImpl.parse(str, naming);
  }
}
