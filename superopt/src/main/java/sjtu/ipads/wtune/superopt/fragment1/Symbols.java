package sjtu.ipads.wtune.superopt.fragment1;

import java.util.Collection;

public interface Symbols {
  void bindSymbol(Op op);

  Symbol symbolAt(Op op, Symbol.Kind kind, int oridinal);

  Collection<Symbol> symbolsOf(Symbol.Kind kind);

  Op ownerOf(Symbol.Kind kind, Symbol symbol);

  static Symbols mk() {
    return new SymbolsImpl();
  }

  static Symbols merge(Symbols symbols0, Symbols symbols1) {
    return SymbolsImpl.merge(symbols0, symbols1);
  }
}
