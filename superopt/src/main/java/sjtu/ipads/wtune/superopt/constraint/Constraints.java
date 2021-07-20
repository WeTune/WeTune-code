package sjtu.ipads.wtune.superopt.constraint;

import java.util.List;
import java.util.Set;
import sjtu.ipads.wtune.superopt.fragment1.Symbol;
import sjtu.ipads.wtune.superopt.fragment1.SymbolNaming;

public interface Constraints extends List<Constraint> {
  Set<Symbol> eqClassOf(Symbol symbol);

  Symbol sourceOf(Symbol attrSym);

  Iterable<Constraint> uniqueKeys();

  Iterable<Constraint> foreignKeys();

  StringBuilder stringify(SymbolNaming naming, StringBuilder builder);

  default String stringify(SymbolNaming naming) {
    return stringify(naming, new StringBuilder()).toString();
  }

  static Constraints mk(List<Constraint> constraints) {
    return ConstraintsImpl.mk(constraints);
  }
}
