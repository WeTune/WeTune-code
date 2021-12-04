package sjtu.ipads.wtune.superopt.constraint;

import sjtu.ipads.wtune.common.utils.NaturalCongruence;
import sjtu.ipads.wtune.superopt.fragment.Symbol;
import sjtu.ipads.wtune.superopt.fragment.SymbolNaming;
import sjtu.ipads.wtune.superopt.fragment.Symbols;

import java.util.List;
import java.util.Set;

public interface Constraints extends List<Constraint> {
  boolean isEq(Symbol s0, Symbol s1);

  Set<Symbol> eqClassOf(Symbol symbol);

  Symbol sourceOf(Symbol attrSym);

  List<Constraint> ofKind(Constraint.Kind kind);

  StringBuilder canonicalStringify(SymbolNaming naming, StringBuilder builder);

  StringBuilder stringify(SymbolNaming naming, StringBuilder builder);

  NaturalCongruence<Symbol> eqSymbols();

  Symbol instantiationSourceOf(Symbol tgtSym);

  default String stringify(SymbolNaming naming) {
    return stringify(naming, new StringBuilder()).toString();
  }

  /** srcSyms: symbols at the source side. */
  static Constraints mk(Symbols srcSyms, List<Constraint> constraints) {
    return ConstraintsImpl.mk(srcSyms, constraints);
  }
}
