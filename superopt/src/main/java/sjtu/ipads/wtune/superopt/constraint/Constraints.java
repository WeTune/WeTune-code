package sjtu.ipads.wtune.superopt.constraint;

import sjtu.ipads.wtune.common.utils.NaturalCongruence;
import sjtu.ipads.wtune.superopt.fragment.Symbol;
import sjtu.ipads.wtune.superopt.fragment.SymbolNaming;

import java.util.List;
import java.util.Set;

public interface Constraints extends List<Constraint> {
  boolean isEq(Symbol s0, Symbol s1);

  Set<Symbol> eqClassOf(Symbol symbol);

  Symbol sourceOf(Symbol attrSym);

  List<Constraint> ofKind(Constraint.Kind kind);

  StringBuilder canonicalStringify(SymbolNaming naming, StringBuilder builder);

  StringBuilder stringify(SymbolNaming naming, StringBuilder builder);

  NaturalCongruence<Symbol> congruence();

  default String stringify(SymbolNaming naming) {
    return stringify(naming, new StringBuilder()).toString();
  }

  static Constraints mk(List<Constraint> constraints) {
    return ConstraintsImpl.mk(constraints);
  }
}
