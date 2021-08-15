package sjtu.ipads.wtune.superopt.constraint;

import sjtu.ipads.wtune.superopt.fragment1.Symbol;
import sjtu.ipads.wtune.superopt.fragment1.SymbolNaming;

import java.util.List;
import java.util.Set;

public interface Constraints extends List<Constraint> {
  boolean isEq(Symbol s0, Symbol s1);

  Set<Symbol> eqClassOf(Symbol symbol);

  Symbol sourceOf(Symbol attrSym);

  Iterable<Constraint> ofKind(Constraint.Kind kind);

  StringBuilder canonicalStringify(SymbolNaming naming, StringBuilder builder);

  StringBuilder stringify(SymbolNaming naming, StringBuilder builder);

  default String stringify(SymbolNaming naming) {
    return stringify(naming, new StringBuilder()).toString();
  }

  static Constraints mk(List<Constraint> constraints) {
    return ConstraintsImpl.mk(constraints);
  }
}
