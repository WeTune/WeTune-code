package sjtu.ipads.wtune.superopt.fragment1;

import java.util.List;

public interface Symbols {
  int size();

  void bindSymbol(Op op);

  Symbol symbolAt(Op op, Symbol.Kind kind, int ordinal);

  List<Symbol> symbolsOf(Symbol.Kind kind);

  Op ownerOf(Symbol symbol);

  static Symbols mk() {
    return new SymbolsImpl();
  }

  static Symbols merge(Symbols symbols0, Symbols symbols1) {
    return SymbolsImpl.merge(symbols0, symbols1);
  }
}
