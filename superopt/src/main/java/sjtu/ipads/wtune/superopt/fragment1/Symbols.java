package sjtu.ipads.wtune.superopt.fragment1;

import java.util.Collection;

public interface Symbols {
  void bindSymbol(Op op);

  Symbol symbolAt(Op op, Symbol.Kind kind, int oridinal);

  Collection<Symbol> symbolsOf(Symbol.Kind kind);

  static Symbols mk() {
    return new SymbolsImpl();
  }
}
